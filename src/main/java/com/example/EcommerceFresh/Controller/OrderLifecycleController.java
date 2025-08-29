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
        if(!order.getOrderStatus().equalsIgnoreCase("Pending") && !order.getOrderStatus().toLowerCase().contains("awaiting")){
            return ResponseEntity.badRequest().body("Cannot cancel at this stage");
        }
        order.setOrderStatus("Cancelled");
        userOrderDao.save(order);
        return ResponseEntity.ok("Cancelled");
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
        if(!type.equalsIgnoreCase("RETURN") && !type.equalsIgnoreCase("REPLACE")) return ResponseEntity.badRequest().body("Invalid type");
        ReturnRequest rr = new ReturnRequest();
        rr.setOrder(order);
        rr.setRequestType(type.toUpperCase());
        rr.setReason(reason);
        rr.setStatus("Pending");
        returnRequestDao.save(rr);
        // mark order status
        order.setOrderStatus(type.equalsIgnoreCase("RETURN")?"ReturnRequested":"ReplaceRequested");
        userOrderDao.save(order);
        return ResponseEntity.ok("Request submitted");
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
        userOrderDao.save(order);
        otp.setUsed(true);
        deliveryOtpDao.save(otp);
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
        // action: RETURN or REPLACE or REJECT
        if(action == null) action = "RETURN";
        if(action.equalsIgnoreCase("REJECT")){
            req.setStatus("Rejected");
            req.setRequestType(req.getRequestType());
            returnRequestDao.save(req);
            order.setOrderStatus("ReturnRejected");
            userOrderDao.save(order);
            return ResponseEntity.ok("Rejected");
        }
        if(action.equalsIgnoreCase("REPLACE")){
            req.setStatus("Approved");
            req.setRequestType("REPLACE");
            returnRequestDao.save(req);
            order.setOrderStatus("ReplaceApproved");
            userOrderDao.save(order);
            return ResponseEntity.ok("Replace Approved");
        }
        // default: RETURN
        req.setStatus("Approved");
        req.setRequestType("RETURN");
        returnRequestDao.save(req);
        order.setOrderStatus("ReturnApproved");
        userOrderDao.save(order);
        return ResponseEntity.ok("Return Approved");
    }

    // Admin page to view and manage orders (generate OTP)
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrdersPage(Model model){
        // Show order groups instead of individual orders for better organization
        java.util.List<OrderGroup> orderGroups = orderGroupDao.findAll();
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("orderGroups", orderGroups);
        
        // For OTP generation, we still need to work with individual orders
        // compute orders that already have an active OTP so template can disable generate button
        java.util.Set<Integer> ordersWithActiveOtp = deliveryOtpDao.findAll().stream()
                .filter(d -> d.getOrder() != null)
                .filter(d -> !d.isUsed())
                .filter(d -> d.getExpiresAt() == null || d.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(d -> d.getOrder().getId())
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("ordersWithActiveOtp", ordersWithActiveOtp);
        return "adminOrders";
    }
}
