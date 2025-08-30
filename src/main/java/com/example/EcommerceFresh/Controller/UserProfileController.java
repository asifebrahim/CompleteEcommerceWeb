package com.example.EcommerceFresh.Controller;

import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserProfileDao;
import com.example.EcommerceFresh.Dao.OrderGroupDao;
import com.example.EcommerceFresh.Dao.ReturnRequestDao;
import com.example.EcommerceFresh.Entity.UserProfile;
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
    private OrderGroupDao orderGroupDao;
    
    @Autowired
    private ReturnRequestDao returnRequestDao;
    
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

        // Build a map to indicate whether a return is allowed for each individual user order (by order id)
        Map<Integer, Boolean> returnAllowedMap = new HashMap<>();
        // Collect orders that already have a pending return request so we can hide button for them
        java.util.Set<Integer> pendingReturnOrderIds = new java.util.HashSet<>();
        try {
            java.util.List<com.example.EcommerceFresh.Entity.ReturnRequest> pending = returnRequestDao.findByStatus("Pending");
            for (com.example.EcommerceFresh.Entity.ReturnRequest rr : pending) {
                if (rr.getOrder() != null && rr.getOrder().getId() != null) pendingReturnOrderIds.add(rr.getOrder().getId());
            }
        } catch(Exception ex){ /* ignore if DAO unavailable */ }

        for (OrderGroup og : orderGroups) {
            if (og.getUserOrders() == null) continue;
            for (com.example.EcommerceFresh.Entity.UserOrder uo : og.getUserOrders()){
                boolean allowed = false;
                if (uo.getDeliveredAt() != null) {
                    Duration sinceDelivered = Duration.between(uo.getDeliveredAt(), LocalDateTime.now());
                    if (sinceDelivered.toDays() <= 7) {
                        allowed = true;
                    }
                }
                // hide if there's already a pending return request for this specific order
                if (pendingReturnOrderIds.contains(uo.getId())) allowed = false;
                returnAllowedMap.put(uo.getId(), allowed);
            }
        }

        // Build a map of orderId -> return request status (if any) so UI can display status labels
        Map<Integer, String> returnStatusMap = new HashMap<>();
        try {
            java.util.List<com.example.EcommerceFresh.Entity.ReturnRequest> allReqs = returnRequestDao.findAll();
            for (com.example.EcommerceFresh.Entity.ReturnRequest rr : allReqs) {
                if (rr.getOrder() != null && rr.getOrder().getUser() != null && rr.getOrder().getUser().getEmail() != null
                        && rr.getOrder().getUser().getEmail().equals(user.getEmail())) {
                    Integer oid = rr.getOrder().getId();
                    if (oid != null && !returnStatusMap.containsKey(oid)) {
                        returnStatusMap.put(oid, rr.getStatus());
                    }
                }
            }
        } catch (Exception ex) { /* ignore */ }
        
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("orderGroups", orderGroups);
        model.addAttribute("returnAllowedMap", returnAllowedMap);
        model.addAttribute("returnStatusMap", returnStatusMap);
        return "userOrders";
    }
}
