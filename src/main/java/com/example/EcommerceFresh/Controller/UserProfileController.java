package com.example.EcommerceFresh.Controller;

import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserProfileDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Dao.OrderGroupDao;
import com.example.EcommerceFresh.Entity.UserProfile;
import com.example.EcommerceFresh.Entity.UserOrder;
import com.example.EcommerceFresh.Entity.OrderGroup;
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
import java.util.Map;
import java.util.HashMap;
import java.time.Duration;
import java.time.LocalDateTime;

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
    private OrderGroupDao orderGroupDao;
    
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
        
        // Fetch grouped orders instead of individual user orders
        List<OrderGroup> orderGroups = orderGroupDao.findByUserOrderByCreatedAtDesc(user);

        // Build a map to indicate whether a return is allowed for each order group
        Map<Integer, Boolean> returnAllowedMap = new HashMap<>();
        for (OrderGroup og : orderGroups) {
            boolean allowed = false;
            if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                UserOrder firstOrder = og.getUserOrders().get(0);
                if (firstOrder.getDeliveredAt() != null) {
                    Duration sinceDelivered = Duration.between(firstOrder.getDeliveredAt(), LocalDateTime.now());
                    if (sinceDelivered.toDays() <= 5) {
                        allowed = true;
                    }
                }
            }
            returnAllowedMap.put(og.getId(), allowed);
        }

        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("orderGroups", orderGroups);
        model.addAttribute("returnAllowedMap", returnAllowedMap);
        return "userOrders";
    }
}
