package com.example.EcommerceFresh.Controller;

import com.example.EcommerceFresh.Dao.DeliveryOtpDao;
import com.example.EcommerceFresh.Dao.OrderGroupDao;
import com.example.EcommerceFresh.Dao.ReturnRequestDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Dao.WishlistDao;
import com.example.EcommerceFresh.Entity.DeliveryOtp;
import com.example.EcommerceFresh.Entity.OrderGroup;
import com.example.EcommerceFresh.Entity.ReturnRequest;
import com.example.EcommerceFresh.Entity.UserOrder;
import com.example.EcommerceFresh.Entity.Users;
import com.example.EcommerceFresh.Entity.Wishlist;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.ProductDao;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Random;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.stream.Collectors;

@Controller
public class OrderLifecycleController {

    @Autowired
    private UserOrderDao userOrderDao;

    @Autowired
    private WishlistDao wishlistDao;

    @Autowired
    private ReturnRequestDao returnRequestDao;

    @Autowired
    private DeliveryOtpDao deliveryOtpDao;
    
    @Autowired
    private OrderGroupDao orderGroupDao;

    @Autowired
    private UserDao usersRepository;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private OtpService otpService;

    // Add/remove wishlist
    @PostMapping("/wishlist/add/{productId}")
    public ResponseEntity<String> addToWishlist(@PathVariable Integer productId, Principal principal){
        if(principal==null) return ResponseEntity.status(401).body("Unauthorized");
        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        if(user == null) return ResponseEntity.status(401).body("Unauthorized");
        // check existing
        java.util.List<Wishlist> list = wishlistDao.findByUser(user);
        boolean exists = list.stream().anyMatch(w -> w.getProduct() != null && w.getProduct().getId() == productId);
        if(exists) return ResponseEntity.ok("Already in wishlist");
        Wishlist w = new Wishlist();
        w.setUser(user);
        var pOpt = productDao.findById(productId);
        if (pOpt.isEmpty()) {
            // avoid saving a transient Product with only id; require existing product
            return ResponseEntity.badRequest().body("Product not found");
        }
        w.setProduct(pOpt.get());
        wishlistDao.save(w);
        return ResponseEntity.ok("Added");
    }

    @PostMapping("/wishlist/remove/{productId}")
    public ResponseEntity<String> removeFromWishlist(@PathVariable Integer productId, Principal principal){
        if(principal==null) return ResponseEntity.status(401).body("Unauthorized");
        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        if(user == null) return ResponseEntity.status(401).body("Unauthorized");
        // Prefer direct DAO lookup (uses nested property query)
        try{
            Wishlist byPair = wishlistDao.findByUserAndProduct_Id(user, productId);
            if(byPair != null){
                wishlistDao.delete(byPair);
                return ResponseEntity.ok("Removed");
            }
        } catch(Exception ignored){ }
        // Fallback: scan user's wishlist
        java.util.List<Wishlist> list = wishlistDao.findByUser(user);
        var existing = list.stream().filter(w -> w.getProduct() != null && w.getProduct().getId() == productId).findFirst();
        if(existing.isEmpty()) return ResponseEntity.ok("Not found");
        wishlistDao.delete(existing.get());
        return ResponseEntity.ok("Removed");
    }

    // Form endpoints for browser usage
    @PostMapping("/wishlist/add/{productId}/form")
    public String addToWishlistForm(@PathVariable Integer productId, Principal principal){
        addToWishlist(productId, principal);
        return "redirect:/wishlist";
    }

    @PostMapping("/wishlist/remove/{productId}/form")
    public String removeFromWishlistForm(@PathVariable Integer productId, Principal principal){
        removeFromWishlist(productId, principal);
        return "redirect:/wishlist";
    }

    @GetMapping("/wishlist")
    public String wishlistPage(Model model, Principal principal){
        if(principal == null) return "redirect:/login";
        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        if(user == null) return "redirect:/login";
        java.util.List<Wishlist> items = wishlistDao.findByUser(user);
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("items", items);
        return "wishlist";
    }

    // Cancel order (user)
    @PostMapping("/order/cancel/{orderId}")
    public ResponseEntity<String> cancelOrder(@PathVariable Integer orderId, Principal principal){
        if(principal==null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<UserOrder> o = userOrderDao.findById(orderId);
        if(o.isEmpty()) return ResponseEntity.badRequest().body("Order not found");
        UserOrder order = o.get();
        if(!order.getUser().getEmail().equals(principal.getName())) return ResponseEntity.status(403).body("Forbidden");
        String status = order.getOrderStatus() == null ? "" : order.getOrderStatus();
        // Only allow cancelling while pending/awaiting
        if(!status.equalsIgnoreCase("Pending") && !status.toLowerCase().contains("awaiting")){
            return ResponseEntity.badRequest().body("Cannot cancel at this stage");
        }
        // Disallow cancelling delivered orders here
        if(status.equalsIgnoreCase("Delivered")){
            return ResponseEntity.badRequest().body("Cannot cancel delivered order; request a return instead");
        }

        // If part of a group, delete all orders in the group and mark group cancelled
        OrderGroup og = order.getOrderGroup();
        if(og != null){
            java.util.List<UserOrder> toDelete = og.getUserOrders() != null ? new java.util.ArrayList<>(og.getUserOrders()) : java.util.Collections.emptyList();
            for(UserOrder uo : toDelete){
                try { userOrderDao.delete(uo); } catch(Exception ex){ ex.printStackTrace(); }
            }
            og.setGroupStatus("Cancelled");
            orderGroupDao.save(og);

            // mark any active delivery OTPs for orders in this group as used so they no longer appear in delivery dashboard
            deliveryOtpDao.findAll().stream()
                    .filter(d -> d.getOrder() != null && d.getOrder().getOrderGroup() != null && og.getId() != null && d.getOrder().getOrderGroup().getId().equals(og.getId()))
                    .forEach(d -> { d.setUsed(true); deliveryOtpDao.save(d); });
        } else {
            // delete single order
            try { userOrderDao.delete(order); } catch(Exception ex){ ex.printStackTrace(); }
            // mark any OTPs tied to this order used
            deliveryOtpDao.findAll().stream()
                    .filter(d -> d.getOrder() != null && d.getOrder().getId().equals(order.getId()))
                    .forEach(d -> { d.setUsed(true); deliveryOtpDao.save(d); });
        }

        return ResponseEntity.ok("Cancelled and removed");
    }

    // Form wrapper for cancel that provides a flash message and redirect
    @PostMapping("/order/cancel/{orderId}/form")
    public String cancelOrderForm(@PathVariable Integer orderId, Principal principal, RedirectAttributes redirectAttributes){
        ResponseEntity<String> resp = cancelOrder(orderId, principal);
        if(resp.getStatusCode().is2xxSuccessful()){
            redirectAttributes.addFlashAttribute("success", resp.getBody());
        } else {
            redirectAttributes.addFlashAttribute("error", resp.getBody());
        }
        return "redirect:/profile/orders";
    }

    // Request return/replace
    @PostMapping("/order/return/{orderId}")
    public ResponseEntity<String> requestReturn(@PathVariable Integer orderId, @RequestParam String type, @RequestParam(required=false) String reason, Principal principal){
        if(principal==null) return ResponseEntity.status(401).body("Unauthorized");
        Optional<UserOrder> o = userOrderDao.findById(orderId);
        if(o.isEmpty()) return ResponseEntity.badRequest().body("Order not found");
        UserOrder order = o.get();
        if(!order.getUser().getEmail().equals(principal.getName())) return ResponseEntity.status(403).body("Forbidden");
        // Only RETURN is supported; REPLACE is not available
        if (!type.equalsIgnoreCase("RETURN")) {
            return ResponseEntity.badRequest().body("Only RETURN requests are supported");
        }

        // Ensure order has been delivered and is within the 5-day return window
        if (order.getDeliveredAt() == null) {
            return ResponseEntity.badRequest().body("Order has not been delivered yet");
        }
        java.time.Duration sinceDelivered = java.time.Duration.between(order.getDeliveredAt(), java.time.LocalDateTime.now());
        if (sinceDelivered.toDays() > 5) {
            return ResponseEntity.badRequest().body("Return window expired");
        }

        // create return request
        ReturnRequest rr = new ReturnRequest();
        rr.setOrder(order);
        rr.setRequestType("RETURN");
        rr.setReason(reason);
        rr.setStatus("Pending");
        returnRequestDao.save(rr);
        // mark order status
        order.setOrderStatus("ReturnRequested");
        userOrderDao.save(order);
        return ResponseEntity.ok("Return request submitted");
    }

    // Form wrapper for return/replace that provides flash message and redirect
    @PostMapping("/order/return/{orderId}/form")
    public String requestReturnForm(@PathVariable Integer orderId, @RequestParam String type, @RequestParam(required=false) String reason, Principal principal, RedirectAttributes redirectAttributes){
        ResponseEntity<String> resp = requestReturn(orderId, type, reason, principal);
        if(resp.getStatusCode().is2xxSuccessful()){
            redirectAttributes.addFlashAttribute("success", resp.getBody());
        } else {
            redirectAttributes.addFlashAttribute("error", resp.getBody());
        }
        return "redirect:/profile/orders";
    }

    // Delivery OTP creation by admin when marking as out for delivery
    @PostMapping("/admin/order/generate-otp/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateDeliveryOtp(@PathVariable Integer orderId){
        Optional<UserOrder> o = userOrderDao.findById(orderId);
        if (o.isEmpty()) return ResponseEntity.badRequest().body("Order not found");
        UserOrder order = o.get();

        // Do not generate OTP for already delivered orders
        if (order.getOrderStatus() != null && order.getOrderStatus().toLowerCase().contains("delivered")) {
            return ResponseEntity.badRequest().body("Order already delivered");
        }

        // Check for existing active (not used and not expired) OTP for this order
        Optional<DeliveryOtp> existingActive = deliveryOtpDao.findAll().stream()
                .filter(d -> d.getOrder() != null && d.getOrder().getId() == orderId)
                .filter(d -> !d.isUsed())
                .filter(d -> d.getExpiresAt() == null || d.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst();

        String code;
        if (existingActive.isPresent()) {
            // Re-send existing OTP
            DeliveryOtp otp = existingActive.get();
            code = otp.getOtpCode();
            try {
                if (order.getUser() != null && order.getUser().getEmail() != null && !order.getUser().getEmail().isBlank()) {
                    otpService.sendOtpEmail(order.getUser().getEmail(), code);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            // ensure order status indicates out for delivery
            order.setOrderStatus("Out for Delivery");
            userOrderDao.save(order);
            return ResponseEntity.ok("OTP_SENT:" + code);
        }

        // create new OTP
        DeliveryOtp otp = new DeliveryOtp();
        otp.setOrder(order);
        code = String.valueOf(100000 + new Random().nextInt(900000));
        otp.setOtpCode(code);
        otp.setExpiresAt(LocalDateTime.now().plusDays(14));
        deliveryOtpDao.save(otp);

        // send email notification to customer (best-effort)
        try {
            if (order.getUser() != null && order.getUser().getEmail() != null && !order.getUser().getEmail().isBlank()) {
                otpService.sendOtpEmail(order.getUser().getEmail(), code);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // mark order as out for delivery so UI can reflect it
        order.setOrderStatus("Out for Delivery");
        userOrderDao.save(order);
        return ResponseEntity.ok("OTP:" + code);
    }

    // Admin page to view pending return/replace requests
    @GetMapping("/admin/returns")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewReturnRequests(Model model){
        java.util.List<ReturnRequest> requests = returnRequestDao.findByStatus("Pending");
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("requests", requests);
        return "adminReturns";
    }

    // Delivery dashboard for delivery role to view pending OTPs
    @GetMapping("/delivery/dashboard")
    @PreAuthorize("hasRole('DELIVERY')")
    public String deliveryDashboard(Model model){
        java.util.List<DeliveryOtp> otps = deliveryOtpDao.findAll().stream().filter(d -> !d.isUsed()).collect(Collectors.toList());
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("otps", otps);
        return "deliveryDashboard";
    }

    // Delivery agent verifies OTP
    @PostMapping("/delivery/verify/{orderId}")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<String> verifyDeliveryOtp(@PathVariable Integer orderId, @RequestParam String otpCode){
        Optional<DeliveryOtp> d = deliveryOtpDao.findByOrderIdAndOtpCode(orderId, otpCode);
        if(d.isEmpty()) return ResponseEntity.badRequest().body("Invalid OTP");
        DeliveryOtp otp = d.get();
        if(otp.isUsed()) return ResponseEntity.badRequest().body("OTP already used");
        if(otp.getExpiresAt().isBefore(LocalDateTime.now())) return ResponseEntity.badRequest().body("OTP expired");
        // mark delivered
        UserOrder order = otp.getOrder();
        order.setOrderStatus("Delivered");
        order.setDeliveredAt(LocalDateTime.now());
        userOrderDao.save(order);
        otp.setUsed(true);
        deliveryOtpDao.save(otp);

        // If this order is part of an OrderGroup, check if all group orders are delivered -> update group status
        OrderGroup og = order.getOrderGroup();
        if (og != null) {
            boolean allDelivered = true;
            if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                for (UserOrder uo : og.getUserOrders()) {
                    String st = uo.getOrderStatus() == null ? "" : uo.getOrderStatus();
                    if (!st.toLowerCase().contains("delivered")) { allDelivered = false; break; }
                }
            } else {
                allDelivered = false;
            }
            if (allDelivered) {
                og.setGroupStatus("Delivered");
                orderGroupDao.save(og);
            }
        }
        return ResponseEntity.ok("Verified and delivered");
    }

    // Admin approves/declines return requests
    @PostMapping("/admin/return/approve/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveReturn(@PathVariable Integer requestId, @RequestParam(required=false) String action){
        Optional<ReturnRequest> r = returnRequestDao.findById(requestId);
        if(r.isEmpty()) return ResponseEntity.badRequest().body("Request not found");
        ReturnRequest req = r.get();
        UserOrder order = req.getOrder();
        // action: RETURN or REJECT
        if(action == null) action = "RETURN";
        if(action.equalsIgnoreCase("REJECT")){
            req.setStatus("Rejected");
            returnRequestDao.save(req);
            order.setOrderStatus("ReturnRejected");
            userOrderDao.save(order);

            // mark any OTPs for this order used
            deliveryOtpDao.findAll().stream()
                    .filter(d -> d.getOrder() != null && d.getOrder().getId().equals(order.getId()))
                    .forEach(d -> { d.setUsed(true); deliveryOtpDao.save(d); });

            return ResponseEntity.ok("Rejected");
        }
        // approve return: set request approved and mark order/group as Returned
        req.setStatus("Approved");
        req.setRequestType("RETURN");
        returnRequestDao.save(req);
        order.setOrderStatus("Returned");
        userOrderDao.save(order);

        // if part of a group, mark group status as Returned
        OrderGroup og = order.getOrderGroup();
        if (og != null) {
            og.setGroupStatus("Returned");
            orderGroupDao.save(og);
        }

        deliveryOtpDao.findAll().stream()
                .filter(d -> d.getOrder() != null && d.getOrder().getId().equals(order.getId()))
                .forEach(d -> { d.setUsed(true); deliveryOtpDao.save(d); });

        return ResponseEntity.ok("Return Approved");
    }

    // Admin: view processed (approved) returns
    @GetMapping("/admin/returns/processed")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewProcessedReturns(Model model){
        java.util.List<ReturnRequest> processed = returnRequestDao.findByStatus("Approved");
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("requests", processed);
        return "adminReturned";
    }

    // Admin: mark an approved return as completed (finalize and optionally restock)
    @PostMapping("/admin/return/complete/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> completeReturn(@PathVariable Integer requestId){
        Optional<ReturnRequest> r = returnRequestDao.findById(requestId);
        if(r.isEmpty()) return ResponseEntity.badRequest().body("Request not found");
        ReturnRequest req = r.get();
        if(!"Approved".equalsIgnoreCase(req.getStatus())) return ResponseEntity.badRequest().body("Request not in approved state");
        UserOrder order = req.getOrder();
        // finalize
        req.setStatus("Completed");
        returnRequestDao.save(req);
        order.setOrderStatus("Returned");
        userOrderDao.save(order);
        OrderGroup og = order.getOrderGroup();
        if(og != null){ og.setGroupStatus("Returned"); orderGroupDao.save(og); }

        // mark related OTPs used
        deliveryOtpDao.findAll().stream()
                .filter(d -> d.getOrder() != null && d.getOrder().getId().equals(order.getId()))
                .forEach(d -> { d.setUsed(true); deliveryOtpDao.save(d); });

        return ResponseEntity.ok("Return completed");
    }

    // Delivery staff can assign/verify an email for an order if customer email is missing
    @PostMapping("/delivery/assign-email/{orderId}")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<String> assignDeliveryEmail(@PathVariable Integer orderId, @RequestParam String email){
        if(email == null || email.isBlank()) return ResponseEntity.badRequest().body("Invalid email");
        Optional<UserOrder> o = userOrderDao.findById(orderId);
        if(o.isEmpty()) return ResponseEntity.badRequest().body("Order not found");
        UserOrder order = o.get();
        Users user = order.getUser();
        if(user == null) return ResponseEntity.badRequest().body("Order has no associated user");
        if(user.getEmail() != null && !user.getEmail().isBlank()) return ResponseEntity.ok("Email already present");
        user.setEmail(email);
        usersRepository.save(user);
        return ResponseEntity.ok("Email assigned");
    }

    // Admin view for cancelled order groups
    @GetMapping("/admin/orders/cancelled")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCancelledOrdersPage(Model model){
        java.util.List<OrderGroup> cancelled = orderGroupDao.findAll().stream()
                .filter(og -> og.getGroupStatus() != null && og.getGroupStatus().equalsIgnoreCase("Cancelled"))
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("orderGroups", cancelled);
        return "adminCancelledOrders"; // template may need to be created
    }

    @PostMapping("/admin/order/group/deliver/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> markGroupDelivered(@PathVariable Integer groupId){
        Optional<OrderGroup> ogOpt = orderGroupDao.findById(groupId);
        if(ogOpt.isEmpty()) return ResponseEntity.badRequest().body("Group not found");
        OrderGroup og = ogOpt.get();
        // mark every order in the group as Delivered
        if(og.getUserOrders() != null){
            for(UserOrder uo : og.getUserOrders()){
                uo.setOrderStatus("Delivered");
                uo.setDeliveredAt(java.time.LocalDateTime.now());
                try { userOrderDao.save(uo); } catch(Exception ex){ ex.printStackTrace(); }
                // mark any OTPs for this order used
                deliveryOtpDao.findAll().stream()
                        .filter(d -> d.getOrder() != null && d.getOrder().getId().equals(uo.getId()))
                        .forEach(d -> { d.setUsed(true); deliveryOtpDao.save(d); });
            }
        }
        og.setGroupStatus("Delivered");
        orderGroupDao.save(og);
        return ResponseEntity.ok("Group delivered");
    }

    @GetMapping("/admin/order/group/view/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewOrderGroup(@PathVariable Integer groupId, Model model){
        Optional<OrderGroup> ogOpt = orderGroupDao.findById(groupId);
        if(ogOpt.isEmpty()) return "redirect:/admin/orders";
        OrderGroup og = ogOpt.get();
        model.addAttribute("orderGroup", og);
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "adminOrderGroupView";
    }
}
