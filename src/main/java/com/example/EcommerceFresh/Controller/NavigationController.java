package com.example.EcommerceFresh.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle additional navigation routes that might not exist yet
 */
@Controller
public class NavigationController {
    
    // Handle direct contact routes for backward compatibility
    @GetMapping({"/contact", "/Contact"})
    public String redirectToSupportContact() {
        return "redirect:/support/contact";
    }
    
    @GetMapping("/help")
    public String redirectToSupportHelp() {
        return "redirect:/support/help";
    }
    
    // Handle common navigation routes that might be missing
    @GetMapping({"/returns", "/return"})
    public String showReturnsPage() {
        // For now redirect to support, you can create a dedicated returns page later
        return "redirect:/support/contact";
    }
    
    @GetMapping({"/track-order", "/track", "/order-tracking"})
    public String showTrackOrderPage() {
        // For now redirect to user orders page if authenticated, otherwise to support
        return "redirect:/profile/orders";
    }
}
