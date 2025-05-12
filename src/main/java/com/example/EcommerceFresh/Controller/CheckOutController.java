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

@Controller
public class CheckOutController {

    @Autowired
    private UserDao usersRepository;

    @Autowired
    private PaymentProofDao paymentProofRepository;

    // To render the payment form
    @PostMapping("/payNow")
    public String checkout(@RequestParam("userId") Integer userId, Model model) {
        PaymentProof paymentProof = new PaymentProof();
        model.addAttribute("paymentProof", paymentProof);
        model.addAttribute("userId", userId); // send userId to form
        return "PaymentProof";
    }

    // To handle form submission
    @PostMapping("/ProofUploading")
    public String handlePaymentProof(@ModelAttribute PaymentProof paymentProof,
                                     @RequestParam("userId") Integer userId) {

        Users user = usersRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        paymentProof.setUsers(user);

        paymentProofRepository.save(paymentProof);

        return "redirect:/paymentSuccess"; // or whatever success page
    }
}
