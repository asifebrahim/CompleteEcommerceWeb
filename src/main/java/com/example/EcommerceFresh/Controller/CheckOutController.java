package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.AddressDao;
import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Entity.Address;
import com.example.EcommerceFresh.Entity.PaymentProof;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private AddressDao addressRepository;

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

        return "paymentSuccess"; // or whatever success page
    }
}
