package com.example.EcommerceFresh.Controller;

import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserProfileDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Entity.UserProfile;
import com.example.EcommerceFresh.Entity.UserOrder;
import com.example.EcommerceFresh.Entity.Users;
import com.example.EcommerceFresh.Global.GlobalData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private UserProfileDao userProfileDao;
    
    @Autowired
    private UserOrderDao userOrderDao;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("")
    public String profile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Users user = userDao.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        UserProfile profile = userProfileDao.findByUser(user).orElse(new UserProfile());
        if (profile.getUser() == null) {
            profile.setUser(user);
        }
        
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "userProfile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("profile") UserProfile profile, 
                               @RequestParam(value = "newPassword", required = false) String newPassword,
                               Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Users user = userDao.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        // Update user basic info
        user.setFirstName(profile.getUser().getFirstName());
        user.setLastName(profile.getUser().getLastName());
        user.setEmail(profile.getUser().getEmail());
        
        // Update password if provided
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        
        userDao.save(user);
        
        // Update profile
        profile.setUser(user);
        userProfileDao.save(profile);
        
        redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");
        return "redirect:/profile";
    }

    @GetMapping("/orders")
    public String orders(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Users user = userDao.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        
        List<UserOrder> orders = userOrderDao.findByUserOrderByOrderDateDesc(user);
        
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("orders", orders);
        return "userOrders";
    }
}
