package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.AddressDao;
import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Entity.Address;
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
import java.util.HashMap;
import java.util.Map;

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

    // Create Razorpay order
    @PostMapping("/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrder(@RequestParam Double amount) {
        try {
            Order order = razorpayService.createOrder(amount);
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", razorpayService.getKeyId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create order");
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
                Users user = usersRepository.findByEmail(principal.getName()).orElseThrow();
                
                // Create orders for each cart item
                for (Product product : GlobalData.cart) {
                    UserOrder userOrder = new UserOrder();
                    userOrder.setUser(user);
                    userOrder.setProduct(product);
                    userOrder.setQuantity(1);
                    userOrder.setTotalPrice(product.getPrice());
                    userOrder.setOrderStatus("Paid");
                    userOrderDao.save(userOrder);
                }
                
                // Clear cart
                GlobalData.cart.clear();
                
                response.put("status", "success");
                response.put("message", "Payment verified and order created successfully");
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Payment verified but order creation failed");
            }
        } else {
            response.put("status", "error");
            response.put("message", "Payment verification failed");
        }
        
        return ResponseEntity.ok(response);
    }

    // To render the payment form
    @PostMapping("/payNow")
    public String checkout(Principal principal,
                           Model model,
                           @RequestParam("firstName") String firstName,
                           @RequestParam("lastName") String lastName,
                           @RequestParam("address1") String address1,
                           @RequestParam(value = "address2", required = false) String address2,
                           @RequestParam("pinCode") String pinCode,
                           @RequestParam("town") String town,
                           @RequestParam("phone") String phone,
                           @RequestParam(value = "additionalInfo", required = false) String additionalInfo) {

        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        String email = principal.getName();
        Users user = usersRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        // Create and save address once at checkout
        Address address = new Address();
        address.setFirstName(firstName);
        address.setLastName(lastName);
        address.setAddress1(address1);
        address.setAddress2(address2);
        address.setPinCode(pinCode);
        address.setTown(town);
        address.setPhone(phone);
        address.setEmail(user.getEmail());
        address.setAdditionalInfo(additionalInfo);
        if(addressRepository.findByAddress1(address1).isEmpty()) // Avoid duplicate addresses
        {
            addressRepository.save(address);
        }
        else {
            address = addressRepository.findByAddress1(address1).get();
        }
        // Prepare payment proof bound object with address reference
        PaymentProof paymentProof = new PaymentProof();
        paymentProof.setAddress(address);
        paymentProof.setStatus("Pending");

        model.addAttribute("paymentProof", paymentProof);
        model.addAttribute("address", address);

        return "PaymentProof";
    }

    // To handle form submission
    @PostMapping("/ProofUploading")
    @Transactional
    public String handlePaymentProof(@ModelAttribute PaymentProof paymentProof, Principal principal) {

        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        String email = principal.getName();
        Users user = usersRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        paymentProof.setUsers(user);

        // If address reference present (only id expected), load the persisted address and set it
        if (paymentProof.getAddress() != null && paymentProof.getAddress().getId() > 0) {
            addressRepository.findById(paymentProof.getAddress().getId()).ifPresent(paymentProof::setAddress);
        }

        if (paymentProof.getStatus() == null) {
            paymentProof.setStatus("Pending");
        }

        paymentProofRepository.save(paymentProof);

        // Clear the in-memory cart after successful submission
        try {
            GlobalData.cart.clear();
        } catch (Exception e) {
            // best-effort: swallow to avoid breaking the flow
            e.printStackTrace();
        }

        return "paymentSuccess"; // or whatever success page
    }
}
