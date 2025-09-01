package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.RoleDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserRoleDao;
import com.example.EcommerceFresh.Dao.PendingUserDao;
import com.example.EcommerceFresh.Entity.Roles;
import com.example.EcommerceFresh.Entity.UserRole;
import com.example.EcommerceFresh.Entity.Users;
import com.example.EcommerceFresh.Entity.PendingUser;
import com.example.EcommerceFresh.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Controller
public class LoginController {
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    UserDao userDao;
    @Autowired
    RoleDao roleDao;
    @Autowired
    UserRoleDao userRoleDao;
    @Autowired
    PendingUserDao pendingUserDao;
    @Autowired
    OtpService otpService;

    @Value("${APP_SHOW_OTP:false}")
    private boolean appShowOtp;

    @GetMapping("/login")
    public String login(){
        return "login";
    }
    @GetMapping("/WaitingMessage")
    public String WaitingMessage(Model model){
        return "WaitingMessage";
    }
    @GetMapping("/register")
    public String registerGet(Model model){
        model.addAttribute("user",new Users());
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") Users users, RedirectAttributes redirectAttributes) {
        System.out.println("Register method called with email: " + users.getEmail());

        if(userDao.findByEmail(users.getEmail()).isPresent()){
            System.out.println("User Already Exists: " + users.getEmail());
            redirectAttributes.addFlashAttribute("error","An account with this email already exists.");
            return "redirect:/register";
        }

        // Encrypt the password now and store in pending
        String encodedPassword = bCryptPasswordEncoder.encode(users.getPassword());

        // Create or update PendingUser
        PendingUser pending = pendingUserDao.findByEmail(users.getEmail()).orElse(new PendingUser());
        pending.setFirstName(users.getFirstName());
        pending.setLastName(users.getLastName());
        pending.setEmail(users.getEmail());
        pending.setPassword(encodedPassword);

        // Generate OTP
        String otp = otpService.generateNumericOtp();
        String otpHash = otpService.hashOtp(otp);
        pending.setOtpHash(otpHash);
        pending.setOtpExpiry(otpService.computeExpiry());
        pending.setCreatedAt(LocalDateTime.now());

        pendingUserDao.save(pending);

        // Send email with OTP (async if desired)
        try{
            otpService.sendOtpEmail(pending.getEmail(), otp);
            if(appShowOtp){
                redirectAttributes.addFlashAttribute("devOtp", otp);
            }
        } catch (Exception e){
            e.printStackTrace();
            if(appShowOtp){
                // In dev mode, continue flow and show OTP on verify page
                redirectAttributes.addFlashAttribute("warning","Failed to send OTP via email; showing OTP for development.");
                redirectAttributes.addFlashAttribute("devOtp", otp);
                redirectAttributes.addFlashAttribute("message","OTP generated. Check the field below.");
                return "redirect:/verify?email=" + pending.getEmail();
            }
            redirectAttributes.addFlashAttribute("error","Failed to send OTP. Try again later.");
            return "redirect:/register";
        }

        redirectAttributes.addFlashAttribute("message","OTP sent to your email. Please verify.");
        return "redirect:/verify?email=" + pending.getEmail();
    }

    @GetMapping("/verify")
    public String verifyGet(@RequestParam(name = "email", required = false) String email, Model model){
        model.addAttribute("email", email);
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyPost(@RequestParam String email, @RequestParam String otp, RedirectAttributes redirectAttributes){
        Optional<PendingUser> pendingOpt = pendingUserDao.findByEmail(email);
        if(pendingOpt.isEmpty()){
            redirectAttributes.addFlashAttribute("error","No pending registration found for this email.");
            return "redirect:/register";
        }
        PendingUser pending = pendingOpt.get();
        if(pending.getOtpExpiry().isBefore(LocalDateTime.now())){
            redirectAttributes.addFlashAttribute("error","OTP expired. Please request a new one.");
            return "redirect:/verify?email=" + email;
        }
        if(!otpService.verifyOtp(otp, pending.getOtpHash())){
            redirectAttributes.addFlashAttribute("error","Invalid OTP.");
            return "redirect:/verify?email=" + email;
        }

        // Create real user
        Users users = new Users();
        users.setFirstName(pending.getFirstName());
        users.setLastName(pending.getLastName());
        users.setEmail(pending.getEmail());
        users.setPassword(pending.getPassword()); // already encoded

        Roles userRole = roleDao.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new Roles();
            userRole.setName("ROLE_USER");
            userRole = roleDao.save(userRole);
        }
        Set<Roles> roles = new HashSet<>();
        roles.add(userRole);
        users.setRoles(roles);
        userDao.save(users);

        // delete pending
        pendingUserDao.deleteByEmail(email);

        redirectAttributes.addFlashAttribute("message","Registration complete. You can log in now.");
        return "redirect:/login";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, RedirectAttributes redirectAttributes){
        Optional<PendingUser> pendingOpt = pendingUserDao.findByEmail(email);
        if(pendingOpt.isEmpty()){
            redirectAttributes.addFlashAttribute("error","No pending registration found for this email.");
            return "redirect:/register";
        }
        PendingUser pending = pendingOpt.get();
        String otp = otpService.generateNumericOtp();
        String otpHash = otpService.hashOtp(otp);
        pending.setOtpHash(otpHash);
        pending.setOtpExpiry(otpService.computeExpiry());
        pendingUserDao.save(pending);
        try{
            otpService.sendOtpEmail(email, otp);
        } catch (Exception e){
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error","Failed to send OTP. Try again later.");
            return "redirect:/verify?email=" + email;
        }
        redirectAttributes.addFlashAttribute("message","New OTP sent.");
        return "redirect:/verify?email=" + email;
    }




    @GetMapping("/forgotpassword")
    public String forgotPassword(){
        return "forgotPasswordPage";
    }

    @PostMapping("/resetPassword")
    public String finalResetSubmission(@RequestParam("email") String email, RedirectAttributes redirectAttributes,Model model){
        Optional<Users> userOpt = userDao.findByEmail(email);
        if(userOpt.isPresent()){
            Users user=userOpt.get();
            String otp=otpService.generateNumericOtp();
            String otpHash=otpService.hashOtp(otp);
            user.setOtps(otpHash);
            userDao.save(user);
            try{
                otpService.sendOtpEmail(email,otp);
                redirectAttributes.addFlashAttribute("message","OTP sent to your email.");
                model.addAttribute("email",email);
                return "forgotPasswordOtpVerification";

            }catch(Exception e){
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error","Failed to send OTP. Try again later.");
                return "redirect:/forgotpassword";
            }



        }
        else{
            redirectAttributes.addFlashAttribute("error","No account found with this email.");
            return "redirect:/forgotpassword";
        }

    }
    @PostMapping("/resetPasswordConfirmation")
    public String resetPasswordConfirmation(@RequestParam("email") String email,@RequestParam("otp") String otp, RedirectAttributes redirectAttributes,Model model){
        Optional<Users> userOpt = userDao.findByEmail(email);
        if(userOpt.isPresent()){
            Users user=userOpt.get();
            if(user.getOtps()==null){
                redirectAttributes.addFlashAttribute("error","No OTP request found. Please initiate password reset again.");
                return "redirect:/forgotpassword";
            }
            if(!otpService.verifyOtp(otp,user.getOtps())){
                redirectAttributes.addFlashAttribute("error","Invalid OTP. Please try again.");
                model.addAttribute("email",email);
                return "forgotPasswordOtpVerification";
            }
            // OTP is valid, proceed to reset password page
            model.addAttribute("email",email);
            return "resetPasswordPage";

        }
        else{
            redirectAttributes.addFlashAttribute("error","No account found with this email.");
            return "redirect:/forgotpassword";
        }
    }

    @PostMapping("/saveNewPass")
    public String saveNewPass(@RequestParam("email") String email,@RequestParam("password1") String newPassword,@RequestParam("password2") String confirmPassword, RedirectAttributes redirectAttributes){
        Optional<Users> userOpt = userDao.findByEmail(email);
        if(userOpt.isPresent()){
            Users user=userOpt.get();
            if(!newPassword.equals(confirmPassword)){
                redirectAttributes.addFlashAttribute("message","Passwords do not match.");
                redirectAttributes.addFlashAttribute("email",email);
                return "redirect:/resetPasswordConfirmation?email=" + email;
            }
            String encodedPassword=bCryptPasswordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            user.setOtps(null); // Clear OTP after successful reset
            userDao.save(user);
            redirectAttributes.addFlashAttribute("message","Password reset successful. You can log in now.");
            return "redirect:/login";

        }
        else{
            redirectAttributes.addFlashAttribute("error","No account found with this email.");
            return "redirect:/forgotpassword";
        }
    }



}
