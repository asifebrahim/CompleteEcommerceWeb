package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.AddressDao;
import com.example.EcommerceFresh.Dao.OrderGroupDao;
import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Dao.ProductDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Entity.Address;
import com.example.EcommerceFresh.Entity.OrderGroup;
import com.example.EcommerceFresh.Entity.PaymentProof;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.UserOrder;
import com.example.EcommerceFresh.Entity.Users;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.RazorpayService;
import com.razorpay.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
public class CheckOutController {

    @Autowired
    private UserDao usersRepository;

    @Autowired
    private PaymentProofDao paymentProofRepository;

    @Autowired
    private AddressDao addressRepository;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private UserOrderDao userOrderDao;

    @Autowired
    private OrderGroupDao orderGroupDao;

    @Autowired
    private com.example.EcommerceFresh.Dao.ProductDao productDao;

    // In-memory map to hold cart snapshots keyed by razorpay order id
    private static final ConcurrentHashMap<String, java.util.List<Product>> pendingCartMap = new ConcurrentHashMap<>();

    // Create Razorpay order
    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrder(@RequestParam Double amount) {
        try {
            if (amount == null || amount <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid amount");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            int amountPaise = (int) Math.round(amount * 100);
            final int MIN_PAISA = 100; // ₹1.00
            if (amountPaise < MIN_PAISA) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Amount too small. Minimum allowed is ₹1");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Order order = razorpayService.createOrder(amount);
            String orderId = order.get("id").toString();

            // Store a snapshot of the current cart for this order id
            try {
                pendingCartMap.put(orderId, new ArrayList<>(GlobalData.cart));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", razorpayService.getKeyId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create order: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Verify payment and create order
    @PostMapping("/verify-payment")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyPayment(
            @RequestParam String paymentId,
            @RequestParam String orderId,
            @RequestParam String signature,
            Principal principal) {

        Map<String, String> response = new HashMap<>();

        if (razorpayService.verifyPayment(paymentId, orderId, signature)) {
            // Payment verified successfully
            try {
                if (principal == null) throw new RuntimeException("User not authenticated");
                Users user = usersRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));

                // Use snapshot stored at order creation time
                java.util.List<Product> cartSnapshot = pendingCartMap.remove(orderId);
                if (cartSnapshot == null || cartSnapshot.isEmpty()) {
                    // fallback to live cart
                    cartSnapshot = new ArrayList<>(GlobalData.cart);
                }

                // Create a PaymentProof record for the gateway payment so admin can review/approve it
                try {
                    PaymentProof paymentProof = new PaymentProof();
                    paymentProof.setTransactionId(paymentId);
                    paymentProof.setUsers(user);
                    paymentProof.setStatus("Pending");
                    // Link the first product if available to help admin identify the payment
                    if (cartSnapshot != null && !cartSnapshot.isEmpty()) {
                        Product first = cartSnapshot.get(0);
                        try {
                            var pOpt = productDao.findById(first.getId());
                            if (pOpt.isPresent()) paymentProof.setProduct(pOpt.get());
                            else paymentProof.setProduct(first);
                        } catch (Exception ex) {
                            // fallback to assigning the first product object
                            paymentProof.setProduct(first);
                        }
                    }
                    // persist the payment proof so it appears in admin pending payments
                    paymentProofRepository.save(paymentProof);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Create orders for each cart item and mark them as Paid - Awaiting Dispatch
                try {
                    if (cartSnapshot != null && !cartSnapshot.isEmpty()) {
                        // Create an OrderGroup to group all products from this payment
                        OrderGroup orderGroup = new OrderGroup();
                        orderGroup.setUser(user);
                        orderGroup.setGroupStatus("Paid - Awaiting Dispatch");
                        orderGroup.setRazorpayOrderId(orderId);
                        
                        // Calculate total amount
                        double totalAmount = cartSnapshot.stream().mapToDouble(Product::getPrice).sum();
                        orderGroup.setTotalAmount(totalAmount);
                        
                        // Save the order group first
                        orderGroup = orderGroupDao.save(orderGroup);
                        
                        // Create individual UserOrders for each product and link to the group
                        for (Product product : cartSnapshot) {
                            Product managedProduct = product;
                            try {
                                var pOpt = productDao.findById(product.getId());
                                if (pOpt.isPresent()) managedProduct = pOpt.get();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            UserOrder userOrder = new UserOrder();
                            userOrder.setUser(user);
                            userOrder.setProduct(managedProduct);
                            userOrder.setOrderGroup(orderGroup);
                            userOrder.setQuantity(1);
                            userOrder.setProductPriceAtOrder(managedProduct.getPrice()); // Store price at time of order
                            userOrder.setTotalPrice(managedProduct.getPrice());
                            userOrder.setOrderStatus("Paid - Awaiting Dispatch");
                            userOrderDao.save(userOrder);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Clear the live cart after creating orders
                try { GlobalData.cart.clear(); } catch (Exception ex) { ex.printStackTrace(); }

                response.put("status", "success");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                e.printStackTrace();
                response.put("status", "error");
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } else {
            // Payment verification failed
            response.put("status", "error");
            response.put("message", "Payment verification failed");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        model.addAttribute("user", user);

        // add cart count and total so templates and JS have values
        model.addAttribute("cartCount", GlobalData.cart.size());
        double total = GlobalData.cart.stream().mapToDouble(Product::getPrice).sum();
        model.addAttribute("total", total);

        // Get the user's saved addresses for checkout
        if (user != null) {
            java.util.List<Address> addresses = addressRepository.findAll().stream()
                    .filter(a -> a.getEmail() != null && a.getEmail().equals(user.getEmail()))
                    .collect(Collectors.toList());
            model.addAttribute("addresses", addresses);
        }

        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(
            @RequestParam int addressId,
            @RequestParam String paymentMode,
             Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // Find the address by ID
        Address address = addressRepository.findById(addressId).orElse(null);
        if (address == null || !address.getEmail().equals(user.getEmail())) {
            return "redirect:/checkout"; // Address not found or doesn't belong to the user
        }

        // Create UserOrder entries (one per cart item) with Pending status
        try {
            for (Product prod : new ArrayList<>(GlobalData.cart)) {
                Product managedProduct = prod;
                try {
                    var pOpt = productDao.findById(prod.getId());
                    if (pOpt.isPresent()) managedProduct = pOpt.get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                UserOrder userOrder = new UserOrder();
                userOrder.setUser(user);
                userOrder.setProduct(managedProduct);
                userOrder.setQuantity(1);
                userOrder.setTotalPrice(managedProduct.getPrice());
                userOrder.setOrderStatus("Pending");
                // set delivery address string from Address entity
                String deliveryAddr = String.format("%s, %s, %s", address.getAddress1(), address.getTown(), address.getPinCode());
                userOrder.setDeliveryAddress(deliveryAddr);
                userOrderDao.save(userOrder);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Clear the cart
        try { GlobalData.cart.clear(); } catch (Exception ex) { ex.printStackTrace(); }

        return "redirect:/profile/orders"; // Redirect to profile orders page after successful checkout
    }
}
