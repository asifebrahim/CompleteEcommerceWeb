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

    @Autowired
    private com.example.EcommerceFresh.Service.DiscountService discountService;

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
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) String pinCode,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String additionalInfo,
            Principal principal) {

        Map<String, String> response = new HashMap<>();

        if (razorpayService.verifyPayment(paymentId, orderId, signature)) {
            // Payment verified successfully
            try {
                if (principal == null) throw new RuntimeException("User not authenticated");
                Users user = usersRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));

                // DEBUG: log incoming address fields
                System.out.println("[DEBUG verifyPayment] incoming fields -> firstName:" + firstName + ", lastName:" + lastName + ", address1:" + address1 + ", address2:" + address2 + ", pinCode:" + pinCode + ", town:" + town + ", phone:" + phone + ", email:" + email);

                // Use snapshot stored at order creation time
                java.util.List<Product> cartSnapshot = pendingCartMap.remove(orderId);
                if (cartSnapshot == null || cartSnapshot.isEmpty()) {
                    // fallback to live cart
                    cartSnapshot = new ArrayList<>(GlobalData.cart);
                }

                // Create a PaymentProof record for the gateway payment so admin can review/approve it
                String deliveryAddressStr = null;
                try {
                    // If client provided address fields in the checkout form, prefer those
                    if (address1 != null && !address1.isBlank()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(address1);
                        if (address2 != null && !address2.isBlank()) sb.append(", ").append(address2);
                        if (town != null && !town.isBlank()) sb.append(", ").append(town);
                        if (pinCode != null && !pinCode.isBlank()) sb.append(" - ").append(pinCode);
                        deliveryAddressStr = sb.toString();
                    }

                    System.out.println("[DEBUG verifyPayment] composed deliveryAddressStr=" + deliveryAddressStr);

                    PaymentProof paymentProof = new PaymentProof();
                    paymentProof.setTransactionId(paymentId);
                    paymentProof.setUsers(user);
                    // Mark as Approved immediately (no separate pending stage)
                    paymentProof.setStatus("Approved");

                    // If we don't have deliveryAddress from client, try to find a saved address
                    if (deliveryAddressStr == null || deliveryAddressStr.isBlank()) {
                        try {
                            var addrOpt = addressRepository.findAll().stream()
                                    .filter(a -> a.getEmail() != null && a.getEmail().equals(user.getEmail()))
                                    .findFirst();
                            if (addrOpt.isPresent()) {
                                var addr = addrOpt.get();
                                paymentProof.setAddress(addr);
                                deliveryAddressStr = String.format("%s, %s, %s", addr.getAddress1(), addr.getTown(), addr.getPinCode());
                                System.out.println("[DEBUG verifyPayment] fallback address from Address entity -> " + deliveryAddressStr);
                            } else {
                                System.out.println("[DEBUG verifyPayment] No saved address found for user email: " + user.getEmail());
                                // If no saved address and no form data, create a basic address from whatever we have
                                if ((firstName != null && !firstName.isBlank()) || (phone != null && !phone.isBlank()) || (pinCode != null && !pinCode.isBlank())) {
                                    StringBuilder fallbackSb = new StringBuilder();
                                    if (firstName != null && !firstName.isBlank()) fallbackSb.append(firstName);
                                    if (phone != null && !phone.isBlank()) {
                                        if (fallbackSb.length() > 0) fallbackSb.append(", ");
                                        fallbackSb.append("Phone: ").append(phone);
                                    }
                                    if (pinCode != null && !pinCode.isBlank()) {
                                        if (fallbackSb.length() > 0) fallbackSb.append(", ");
                                        fallbackSb.append("PIN: ").append(pinCode);
                                    }
                                    deliveryAddressStr = fallbackSb.toString();
                                    System.out.println("[DEBUG verifyPayment] created fallback address from form data: " + deliveryAddressStr);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

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
                    // persist the payment proof so it appears in admin payments
                    paymentProofRepository.save(paymentProof);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Create orders for each cart item and mark them as Paid - Awaiting Dispatch
                try {
                    Integer createdGroupId = null;
                    if (cartSnapshot != null && !cartSnapshot.isEmpty()) {
                        // Create an OrderGroup to group all products from this payment
                        OrderGroup orderGroup = new OrderGroup();
                        orderGroup.setUser(user);
                        orderGroup.setGroupStatus("Paid - Awaiting Dispatch");
                        orderGroup.setRazorpayOrderId(orderId);
                        // record gateway transaction id on the group so admin sees it
                        orderGroup.setTransactionId(paymentId);

                        // Calculate total amount with discounts applied
                        double totalAmount = cartSnapshot.stream().mapToDouble(product -> discountService.getEffectivePrice(product)).sum();
                        orderGroup.setTotalAmount(totalAmount);

                        // set delivery address if captured from paymentProof/address or client
                        if (deliveryAddressStr != null && !deliveryAddressStr.isBlank()) {
                            orderGroup.setDeliveryAddress(deliveryAddressStr);
                        }

                        // Store checkout details in OrderGroup - ensure we capture all available data
                        if (firstName != null && !firstName.isBlank()) {
                            orderGroup.setFirstName(firstName);
                        }
                        if (lastName != null && !lastName.isBlank()) {
                            orderGroup.setLastName(lastName);
                        }
                        if (phone != null && !phone.isBlank()) {
                            orderGroup.setMobile(phone);
                        }
                        if (pinCode != null && !pinCode.isBlank()) {
                            orderGroup.setPinCode(pinCode);
                        }
                        if (town != null && !town.isBlank()) {
                            orderGroup.setTown(town);
                        }
                        if (email != null && !email.isBlank()) {
                            orderGroup.setEmailAddress(email);
                        } else {
                            // Fallback to user's email if no email provided in form
                            orderGroup.setEmailAddress(user.getEmail());
                        }

                        // DEBUG: log what will be saved on OrderGroup
                        System.out.println("[DEBUG verifyPayment] OrderGroup before save -> firstName:" + orderGroup.getFirstName() + ", mobile:" + orderGroup.getMobile() + ", pinCode:" + orderGroup.getPinCode() + ", deliveryAddress:" + orderGroup.getDeliveryAddress());

                        // Save the order group first
                        orderGroup = orderGroupDao.save(orderGroup);
                        createdGroupId = orderGroup.getId();

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
                            // Store effective price at time of order (with discount applied)
                            double effectivePrice = discountService.getEffectivePrice(managedProduct);
                            userOrder.setProductPriceAtOrder(effectivePrice);
                            userOrder.setTotalPrice(effectivePrice);
                            userOrder.setOrderStatus("Paid - Awaiting Dispatch");
                            // set delivery address on each order for admin convenience
                            if (deliveryAddressStr != null && !deliveryAddressStr.isBlank()) {
                                userOrder.setDeliveryAddress(deliveryAddressStr);
                            }

                            // Store checkout details in UserOrder - ensure we capture all available data
                            if (firstName != null && !firstName.isBlank()) {
                                userOrder.setFirstName(firstName);
                            }
                            if (lastName != null && !lastName.isBlank()) {
                                userOrder.setLastName(lastName);
                            }
                            if (phone != null && !phone.isBlank()) {
                                userOrder.setMobile(phone);
                            }
                            if (pinCode != null && !pinCode.isBlank()) {
                                userOrder.setPinCode(pinCode);
                            }
                            if (town != null && !town.isBlank()) {
                                userOrder.setTown(town);
                            }
                            if (email != null && !email.isBlank()) {
                                userOrder.setEmailAddress(email);
                            } else {
                                // Fallback to user's email if no email provided in form
                                userOrder.setEmailAddress(user.getEmail());
                            }

                            userOrderDao.save(userOrder);
                        }

                        // include created group id in response so client can redirect to a detail page
                        if (createdGroupId != null) {
                            response.put("orderGroupId", createdGroupId.toString());
                        }

                        // DEBUG: log created group id
                        System.out.println("[DEBUG verifyPayment] createdGroupId=" + createdGroupId);
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
        double total = GlobalData.cart.stream().mapToDouble(product -> discountService.getEffectivePrice(product)).sum();
        model.addAttribute("total", total);

        // Get the user's saved addresses for checkout
        if (user != null) {
            java.util.List<Address> addresses = addressRepository.findAll().stream()
                    .filter(a -> a.getEmail() != null && a.getEmail().equals(user.getEmail()))
                    .collect(Collectors.toList());
            model.addAttribute("addresses", addresses);
            
            // DEBUG: Log if user has saved addresses
            System.out.println("[DEBUG checkout] User " + user.getEmail() + " has " + addresses.size() + " saved addresses");
            if (!addresses.isEmpty()) {
                System.out.println("[DEBUG checkout] First address: " + addresses.get(0).getAddress1() + ", " + addresses.get(0).getTown() + ", " + addresses.get(0).getPinCode());
            }
        }

        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(
            @RequestParam(required = false) Integer addressId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) String pinCode,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String additionalInfo,
             Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // DEBUG: Log checkout attempt - determine if using saved address or form data
        System.out.println("==========================================");
        System.out.println("[FIXED VERSION] User " + user.getEmail() + " attempting checkout");
        System.out.println("[FIXED VERSION] addressId: " + addressId + ", paymentMode: " + paymentMode);
        System.out.println("[FIXED VERSION] Form data -> firstName:" + firstName + ", address1:" + address1 + ", phone:" + phone + ", pinCode:" + pinCode);
        System.out.println("==========================================");

        String deliveryAddr = null;
        String finalFirstName = null;
        String finalLastName = null;
        String finalPhone = null;
        String finalPinCode = null;
        String finalTown = null;
        String finalEmail = null;

        // Try to use saved address first (if addressId provided and valid)
        // if (addressId != null && addressId > 0) {
        //     Address address = addressRepository.findById(addressId).orElse(null);
        //     if (address != null && address.getEmail().equals(user.getEmail())) {
        //         System.out.println("[FIXED VERSION] Using saved address: " + address.getAddress1() + ", " + address.getTown() + ", " + address.getPinCode());
                
        //         // Use saved address data
        //         deliveryAddr = String.format("%s, %s, %s", 
        //             address.getAddress1() == null ? "" : address.getAddress1(), 
        //             address.getTown() == null ? "" : address.getTown(), 
        //             address.getPinCode() == null ? "" : address.getPinCode());
                
        //         finalFirstName = address.getFirstName();
        //         finalLastName = address.getLastName();
        //         finalPhone = address.getPhone();
        //         finalPinCode = address.getPinCode();
        //         finalTown = address.getTown();
        //         finalEmail = address.getEmail();
        //     } else {
        //         System.out.println("[DEBUG COD checkout] Address not found or doesn't belong to user. AddressId: " + addressId + ", User: " + user.getEmail());
        //     }
        // }

        // If no saved address found or addressId not provided, use form data
        // if (deliveryAddr == null) {
        //     System.out.println("[FIXED VERSION] No saved address, trying form data");
            
        //     // Validate required form fields
        //     if (firstName == null || firstName.isBlank() || 
        //         address1 == null || address1.isBlank() || 
        //         phone == null || phone.isBlank() || 
        //         pinCode == null || pinCode.isBlank()) {
        //         System.out.println("[FIXED VERSION] Missing required form fields, redirecting to checkout");
        //         return "redirect:/checkout?error=missing-fields";
        //     }

        //     // Compose delivery address from form data
        //     StringBuilder sb = new StringBuilder();
        //     sb.append(address1);
        //     if (address2 != null && !address2.isBlank()) sb.append(", ").append(address2);
        //     if (town != null && !town.isBlank()) sb.append(", ").append(town);
        //     if (pinCode != null && !pinCode.isBlank()) sb.append(" - ").append(pinCode);
        //     deliveryAddr = sb.toString();

        //     finalFirstName = firstName;
        //     finalLastName = lastName;
        //     finalPhone = phone;
        //     finalPinCode = pinCode;
        //     finalTown = town;
        //     finalEmail = email != null && !email.isBlank() ? email : user.getEmail();
            
        //     System.out.println("[FIXED VERSION] Using form data - deliveryAddr: " + deliveryAddr);
        // }
        StringBuilder sb = new StringBuilder();
            sb.append(address1);
            if (address2 != null && !address2.isBlank()) sb.append(", ").append(address2);
            if (town != null && !town.isBlank()) sb.append(", ").append(town);
            if (pinCode != null && !pinCode.isBlank()) sb.append(" - ").append(pinCode);
            deliveryAddr = sb.toString();

            finalFirstName = firstName;
            finalLastName = lastName;
            finalPhone = phone;
            finalPinCode = pinCode;
            finalTown = town;
            finalEmail = email != null && !email.isBlank() ? email : user.getEmail();
            
            System.out.println("[FIXED VERSION] Using form data - deliveryAddr: " + deliveryAddr);

        // If still no delivery address, redirect back
        // if (deliveryAddr == null || deliveryAddr.isBlank()) {
        //     System.out.println("[FIXED VERSION] No delivery address available, redirecting to checkout");
        //     return "redirect:/checkout";
        // }

        // DEBUG: Log found address
        System.out.println("[DEBUG COD checkout] Final delivery address: " + deliveryAddr);

        // Create an OrderGroup so admin can see grouped orders and address
        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setUser(user);
        // For COD treat as paid-awaiting dispatch; otherwise keep pending until payment verifies
        if (paymentMode != null && paymentMode.equalsIgnoreCase("COD")) {
            orderGroup.setGroupStatus("Paid - Awaiting Dispatch");
        } else {
            orderGroup.setGroupStatus("Pending");
        }
        orderGroup.setDeliveryAddress(deliveryAddr);
        
        // Store checkout details in OrderGroup
        orderGroup.setFirstName(finalFirstName);
        orderGroup.setLastName(finalLastName);
        orderGroup.setMobile(finalPhone);
        orderGroup.setPinCode(finalPinCode);
        orderGroup.setTown(finalTown);
        orderGroup.setEmailAddress(finalEmail);

        // Calculate total amount and save group first with discounts applied
        double totalAmount = GlobalData.cart.stream().mapToDouble(product -> discountService.getEffectivePrice(product)).sum();
        orderGroup.setTotalAmount(totalAmount);
        try {
            orderGroup = orderGroupDao.save(orderGroup);
            System.out.println("[DEBUG COD checkout] Saved OrderGroup with ID: " + orderGroup.getId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Create UserOrder entries (one per cart item) and attach to group
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
                userOrder.setOrderGroup(orderGroup);
                userOrder.setQuantity(1);
                // Store effective price at time of order (with discount applied) - placeOrder method
                double effectivePrice = discountService.getEffectivePrice(managedProduct);
                userOrder.setProductPriceAtOrder(effectivePrice);
                userOrder.setTotalPrice(effectivePrice);
                // set orderStatus based on paymentMode
                if (paymentMode != null && paymentMode.equalsIgnoreCase("COD")) {
                    userOrder.setOrderStatus("Paid - Awaiting Dispatch");
                } else {
                    userOrder.setOrderStatus("Pending");
                }
                // set delivery address string
                userOrder.setDeliveryAddress(deliveryAddr);
                
                // Store checkout details in UserOrder
                userOrder.setFirstName(finalFirstName);
                userOrder.setLastName(finalLastName);
                userOrder.setMobile(finalPhone);
                userOrder.setPinCode(finalPinCode);
                userOrder.setTown(finalTown);
                userOrder.setEmailAddress(finalEmail);
                
                userOrderDao.save(userOrder);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Clear the cart
        try { GlobalData.cart.clear(); } catch (Exception ex) { ex.printStackTrace(); }

        return "redirect:/profile/orders"; // Redirect to profile orders page after successful checkout
    }

    @GetMapping("/paymentSuccess")
    public String paymentSuccess(Model model, Principal principal) {
        // add cart count so header/templates render correctly
        model.addAttribute("cartCount", GlobalData.cart.size());
        // optionally add user info if available
        return "paymentSuccess";
    }
    
    // Validate checkout form fields before payment
    @PostMapping("/validate-checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateCheckout(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String pinCode) {
        
        Map<String, Object> response = new HashMap<>();
        java.util.List<String> missingFields = new java.util.ArrayList<>();
        
        if (firstName == null || firstName.trim().isEmpty()) {
            missingFields.add("First Name");
        }
        if (address1 == null || address1.trim().isEmpty()) {
            missingFields.add("Address Line 1");
        }
        if (phone == null || phone.trim().isEmpty()) {
            missingFields.add("Mobile/Phone");
        }
        if (pinCode == null || pinCode.trim().isEmpty()) {
            missingFields.add("Pin Code");
        }
        
        if (!missingFields.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Please fill the following required fields: " + String.join(", ", missingFields));
            return ResponseEntity.badRequest().body(response);
        }
        
        response.put("valid", true);
        response.put("message", "All required fields are filled");
        return ResponseEntity.ok(response);
    }

    // Handle checkout with form data directly (for users without saved addresses)
    @PostMapping("/checkout-with-form")
    public String placeOrderWithForm(
            @RequestParam String paymentMode,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) String pinCode,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String additionalInfo,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Users user = usersRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        // DEBUG: Log checkout attempt with form data
        System.out.println("[DEBUG checkout-with-form] User " + user.getEmail() + " attempting checkout with paymentMode: " + paymentMode);
        System.out.println("[DEBUG checkout-with-form] Form data -> firstName:" + firstName + ", lastName:" + lastName + ", address1:" + address1 + ", address2:" + address2 + ", pinCode:" + pinCode + ", town:" + town + ", phone:" + phone + ", email:" + email);

        // Validate required fields
        if (firstName == null || firstName.isBlank() || 
            address1 == null || address1.isBlank() || 
            phone == null || phone.isBlank() || 
            pinCode == null || pinCode.isBlank()) {
            System.out.println("[DEBUG checkout-with-form] Missing required fields, redirecting to checkout");
            return "redirect:/checkout?error=missing-fields";
        }

        // Compose delivery address from form data
        StringBuilder sb = new StringBuilder();
        sb.append(address1);
        if (address2 != null && !address2.isBlank()) sb.append(", ").append(address2);
        if (town != null && !town.isBlank()) sb.append(", ").append(town);
        if (pinCode != null && !pinCode.isBlank()) sb.append(" - ").append(pinCode);
        String deliveryAddr = sb.toString();

        System.out.println("[DEBUG checkout-with-form] Composed delivery address: " + deliveryAddr);

        // Create an OrderGroup so admin can see grouped orders and address
        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setUser(user);
        // For COD treat as paid-awaiting dispatch; otherwise keep pending until payment verifies
        if (paymentMode != null && paymentMode.equalsIgnoreCase("COD")) {
            orderGroup.setGroupStatus("Paid - Awaiting Dispatch");
        } else {
            orderGroup.setGroupStatus("Pending");
        }
        orderGroup.setDeliveryAddress(deliveryAddr);
        
        // Store checkout details from form in OrderGroup
        orderGroup.setFirstName(firstName);
        orderGroup.setLastName(lastName);
        orderGroup.setMobile(phone);
        orderGroup.setPinCode(pinCode);
        orderGroup.setTown(town);
        orderGroup.setEmailAddress(email != null && !email.isBlank() ? email : user.getEmail());

        // Calculate total amount and save group first with discounts applied
        double totalAmount = GlobalData.cart.stream().mapToDouble(product -> discountService.getEffectivePrice(product)).sum();
        orderGroup.setTotalAmount(totalAmount);
        try {
            orderGroup = orderGroupDao.save(orderGroup);
            System.out.println("[DEBUG checkout-with-form] Saved OrderGroup with ID: " + orderGroup.getId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Create UserOrder entries (one per cart item) and attach to group
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
                userOrder.setOrderGroup(orderGroup);
                userOrder.setQuantity(1);
                // Store effective price at time of order (with discount applied) - checkout-with-form
                double effectivePrice = discountService.getEffectivePrice(managedProduct);
                userOrder.setProductPriceAtOrder(effectivePrice);
                userOrder.setTotalPrice(effectivePrice);
                // set orderStatus based on paymentMode
                if (paymentMode != null && paymentMode.equalsIgnoreCase("COD")) {
                    userOrder.setOrderStatus("Paid - Awaiting Dispatch");
                } else {
                    userOrder.setOrderStatus("Pending");
                }
                // set delivery address string from form data
                userOrder.setDeliveryAddress(deliveryAddr);
                
                // Store checkout details from form in UserOrder
                userOrder.setFirstName(firstName);
                userOrder.setLastName(lastName);
                userOrder.setMobile(phone);
                userOrder.setPinCode(pinCode);
                userOrder.setTown(town);
                userOrder.setEmailAddress(email != null && !email.isBlank() ? email : user.getEmail());
                
                userOrderDao.save(userOrder);
            }
            System.out.println("[DEBUG checkout-with-form] Created " + GlobalData.cart.size() + " UserOrder entries");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Clear the cart
        try { GlobalData.cart.clear(); } catch (Exception ex) { ex.printStackTrace(); }

        return "redirect:/profile/orders"; // Redirect to profile orders page after successful checkout
    }
}
