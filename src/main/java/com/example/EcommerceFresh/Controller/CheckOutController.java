package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Entity.PaymentProof;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class CheckOutController {

    @Autowired
    private UserDao usersRepository;

    @Autowired
    private PaymentProofDao paymentProofRepository;

    // To render the payment form
    @PostMapping("/payNow")
    public String checkout(Principal principal, Model model){
        PaymentProof paymentProof = new PaymentProof();
        model.addAttribute("paymentProof", paymentProof);

        // If user is authenticated, resolve user id from principal (email)
        if (principal != null) {
            String email = principal.getName();
            Users user = usersRepository.findByEmail(email).orElse(null);
            if (user != null) {
                model.addAttribute("userId", user.getId()); // optional, view can use it if needed
            }
        }

        return "PaymentProof";
    }

    // To handle form submission
    @PostMapping("/ProofUploading")
    public String handlePaymentProof(@ModelAttribute PaymentProof paymentProof, Principal principal) {

        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }
        String email = principal.getName();
        Users user = usersRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        paymentProof.setUsers(user);

        paymentProofRepository.save(paymentProof);

        return "redirect:/paymentSuccess"; // or whatever success page
    }
}
