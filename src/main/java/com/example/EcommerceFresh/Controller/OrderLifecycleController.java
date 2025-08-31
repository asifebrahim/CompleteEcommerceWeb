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
            // Do not change order status on OTP generation/resend; delivery staff will verify OTP to mark delivered
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

        // Do not change order status here; the order will be marked Delivered upon OTP verification by delivery staff
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

        // Ensure deliveryAddress is populated for display in admin list. If missing on the group,
        // try to pick the first non-empty deliveryAddress from the group's UserOrders and persist it.
        // Also collect delivery name/mobile/pin for display (from group or first UserOrder fallback).
        java.util.Map<Integer, java.util.Map<String,String>> groupDeliveryInfo = new java.util.HashMap<>();
        for (OrderGroup og : cancelled) {
            // Use new checkout fields from OrderGroup
            java.util.Map<String,String> info = new java.util.HashMap<>();
            String addr = og.getDeliveryAddress();
            String name = og.getFirstName();
            String mobile = og.getMobile();
            String pin = og.getPinCode();
            
            // If group fields are missing, fallback to first order
            if ((addr == null || addr.isBlank()) || (name == null || name.isBlank()) || 
                (mobile == null || mobile.isBlank()) || (pin == null || pin.isBlank())) {
                if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                    UserOrder uo = og.getUserOrders().get(0);
                    if (addr == null || addr.isBlank()) addr = uo.getDeliveryAddress();
                    if (name == null || name.isBlank()) name = uo.getFirstName();
                    if (mobile == null || mobile.isBlank()) mobile = uo.getMobile();
                    if (pin == null || pin.isBlank()) pin = uo.getPinCode();
                }
            }

            // If still missing name, fall back to user email from first order's user
            if ((name == null || name.isBlank()) && og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                UserOrder uo = og.getUserOrders().get(0);
                if (uo.getUser() != null && uo.getUser().getEmail() != null) {
                    name = uo.getUser().getEmail();
                }
            }

            info.put("address", addr == null ? "" : addr);
            info.put("name", name == null ? "" : name);
            info.put("mobile", mobile == null ? "" : mobile);
            info.put("pin", pin == null ? "" : pin);
            groupDeliveryInfo.put(og.getId() == null ? -1 : og.getId(), info);
        }

        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("orderGroups", cancelled);
        model.addAttribute("groupDeliveryInfo", groupDeliveryInfo);
        return "adminCancelledOrders";
    }

    @GetMapping("/admin/order/group/view/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewOrderGroup(@PathVariable Integer groupId, Model model){
        Optional<OrderGroup> ogOpt = orderGroupDao.findById(groupId);
        if(ogOpt.isEmpty()) return "redirect:/admin/orders";
        OrderGroup og = ogOpt.get();

        // Ensure userOrders are initialized (in case of LAZY fetch) so template can render full details
        if (og.getUserOrders() == null || og.getUserOrders().isEmpty()) {
            try {
                var orders = userOrderDao.findAll().stream()
                        .filter(uo -> uo.getOrderGroup() != null && uo.getOrderGroup().getId() != null && uo.getOrderGroup().getId().equals(og.getId()))
                        .collect(java.util.stream.Collectors.toList());
                if (!orders.isEmpty()) og.setUserOrders(orders);
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        // Provide a deliveryAddress fallback for the template if group-level address is blank
        String deliveryAddress = og.getDeliveryAddress();
        String deliveryName = og.getFirstName();
        String deliveryMobile = og.getMobile();
        String deliveryPin = og.getPinCode();

        // If group fields are missing, fallback to first order
        if ((deliveryAddress == null || deliveryAddress.isBlank()) || 
            (deliveryName == null || deliveryName.isBlank()) ||
            (deliveryMobile == null || deliveryMobile.isBlank()) ||
            (deliveryPin == null || deliveryPin.isBlank())) {
            if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                UserOrder uo = og.getUserOrders().get(0);
                if (deliveryAddress == null || deliveryAddress.isBlank()) {
                    deliveryAddress = uo.getDeliveryAddress();
                }
                if (deliveryName == null || deliveryName.isBlank()) {
                    deliveryName = uo.getFirstName();
                }
                if (deliveryMobile == null || deliveryMobile.isBlank()) {
                    deliveryMobile = uo.getMobile();
                }
                if (deliveryPin == null || deliveryPin.isBlank()) {
                    deliveryPin = uo.getPinCode();
                }
            }
        }

        // If name still missing, prefer the associated user's email as an identifier
        if ((deliveryName == null || deliveryName.isBlank()) && og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
            UserOrder uo = og.getUserOrders().get(0);
            if (uo.getUser() != null && uo.getUser().getEmail() != null) deliveryName = uo.getUser().getEmail();
        }

        model.addAttribute("orderGroup", og);
        model.addAttribute("deliveryAddress", deliveryAddress);
        model.addAttribute("deliveryName", deliveryName);
        model.addAttribute("deliveryMobile", deliveryMobile);
        model.addAttribute("deliveryPin", deliveryPin);
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "adminOrderGroupView";
    }

    // New validation endpoints to ensure required checkout fields exist before attempting payment.
    // These can be called by the frontend prior to initiating Razorpay payment.
    @PostMapping("/checkout/validate/order/{orderId}")
    public ResponseEntity<String> validateOrderBeforePay(@PathVariable Integer orderId){
        Optional<UserOrder> o = userOrderDao.findById(orderId);
        if (o.isEmpty()) return ResponseEntity.badRequest().body("Order not found");
        UserOrder order = o.get();

        java.util.List<String> missing = new java.util.ArrayList<>();
        
        // Check the new checkout fields first
        if (order.getFirstName() == null || order.getFirstName().isBlank()) {
            missing.add("firstName");
        }
        if (order.getMobile() == null || order.getMobile().isBlank()) {
            missing.add("mobile");
        }
        if (order.getPinCode() == null || order.getPinCode().isBlank()) {
            missing.add("pinCode");
        }
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().isBlank()) {
            missing.add("deliveryAddress");
        }

        // Also check linked user email as contact identifier (if available)
        if ((order.getUser() == null || order.getUser().getEmail() == null || order.getUser().getEmail().isBlank())) {
            missing.add("userEmail");
        }

        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields: " + String.join(", ", missing));
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/checkout/validate/group/{groupId}")
    public ResponseEntity<String> validateGroupBeforePay(@PathVariable Integer groupId){
        Optional<OrderGroup> ogOpt = orderGroupDao.findById(groupId);
        if (ogOpt.isEmpty()) return ResponseEntity.badRequest().body("Order group not found");
        OrderGroup og = ogOpt.get();

        java.util.List<String> missing = new java.util.ArrayList<>();
        
        // Check the new checkout fields first
        if (og.getFirstName() == null || og.getFirstName().isBlank()) {
            missing.add("firstName");
        }
        if (og.getMobile() == null || og.getMobile().isBlank()) {
            missing.add("mobile");
        }
        if (og.getPinCode() == null || og.getPinCode().isBlank()) {
            missing.add("pinCode");
        }
        if (og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) {
            missing.add("deliveryAddress");
        }

        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields for group: " + String.join(", ", missing));
        }
        return ResponseEntity.ok("OK");
    }
}
