package com.example.EcommerceFresh.Controller;

import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.ProductDiscount;
import com.example.EcommerceFresh.Service.DiscountService;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import com.example.EcommerceFresh.dto.DiscountDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    @Autowired
    private ProductServiceImpl productService;

    // Show discount management page
    @GetMapping("/discounts")
    public String discountManagement(Model model) {
        List<ProductDiscount> activeDiscounts = discountService.getAllActiveDiscounts();
        List<ProductDiscount> allDiscounts = discountService.getAllDiscounts();
        
        model.addAttribute("activeDiscounts", activeDiscounts);
        model.addAttribute("allDiscounts", allDiscounts);
        model.addAttribute("discountDto", new DiscountDto());
        
        return "adminDiscounts";
    }

    // Search product by ID (AJAX endpoint)
    @GetMapping("/discount/search-product/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchProduct(@PathVariable Integer productId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Product> productOpt = productService.findById(productId);
            if (productOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Product not found with ID: " + productId);
                return ResponseEntity.ok(response);
            }
            
            Product product = productOpt.get();
            
            // Check if product already has active discount
            boolean hasDiscount = discountService.hasActiveDiscount(productId);
            ProductDiscount activeDiscount = null;
            
            if (hasDiscount) {
                activeDiscount = discountService.getActiveDiscount(productId).orElse(null);
            }
            
            response.put("success", true);
            
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("price", product.getPrice());
            productMap.put("category", product.getCategory().getName());
            productMap.put("imageName", product.getImageName());
            response.put("product", productMap);
            response.put("hasActiveDiscount", hasDiscount);
            
            if (activeDiscount != null) {
                Map<String, Object> discountMap = new HashMap<>();
                discountMap.put("id", activeDiscount.getId());
                discountMap.put("discountPrice", activeDiscount.getDiscountPrice());
                discountMap.put("discountPercentage", String.format("%.1f", activeDiscount.getDiscountPercentage()));
                discountMap.put("endDate", activeDiscount.getEndDate().toString());
                discountMap.put("timeRemaining", activeDiscount.getTimeRemaining());
                response.put("activeDiscount", discountMap);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error searching for product: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // Create new discount
    @PostMapping("/discount/create")
    public String createDiscount(@ModelAttribute DiscountDto discountDto, 
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = auth.getName();
            
            // Set start date to now
            discountDto.setStartDate(LocalDateTime.now());
            
            ProductDiscount discount = discountService.createDiscount(discountDto, adminEmail);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Discount created successfully for product ID " + discountDto.getProductId() + 
                "! Discount: ₹" + discount.getOriginalPrice() + " → ₹" + discount.getDiscountPrice() + 
                " (" + String.format("%.1f", discount.getDiscountPercentage()) + "% off)");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating discount: " + e.getMessage());
        }
        
        return "redirect:/admin/discounts";
    }

    // Remove discount
    @PostMapping("/discount/remove/{discountId}")
    public String removeDiscount(@PathVariable Integer discountId, 
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<ProductDiscount> discount = discountService.getDiscountById(discountId);
            if (discount.isPresent()) {
                boolean removed = discountService.removeDiscount(discountId);
                if (removed) {
                    redirectAttributes.addFlashAttribute("successMessage", 
                        "Discount removed successfully for product: " + discount.get().getProduct().getName());
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove discount");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Discount not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error removing discount: " + e.getMessage());
        }
        
        return "redirect:/admin/discounts";
    }

    // Get discount details (AJAX)
    @GetMapping("/discount/details/{discountId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDiscountDetails(@PathVariable Integer discountId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<ProductDiscount> discountOpt = discountService.getDiscountById(discountId);
            if (discountOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Discount not found");
                return ResponseEntity.ok(response);
            }
            
            ProductDiscount discount = discountOpt.get();
            Product product = discount.getProduct();
            
            response.put("success", true);
            
            Map<String, Object> discountMap = new HashMap<>();
            discountMap.put("id", discount.getId());
            discountMap.put("originalPrice", discount.getOriginalPrice());
            discountMap.put("discountPrice", discount.getDiscountPrice());
            discountMap.put("discountPercentage", String.format("%.1f", discount.getDiscountPercentage()));
            discountMap.put("startDate", discount.getStartDate().toString());
            discountMap.put("endDate", discount.getEndDate().toString());
            discountMap.put("isActive", discount.getIsActive());
            discountMap.put("isCurrentlyActive", discount.isCurrentlyActive());
            discountMap.put("timeRemaining", discount.getTimeRemaining());
            discountMap.put("createdBy", discount.getCreatedBy());
            discountMap.put("createdAt", discount.getCreatedAt().toString());
            response.put("discount", discountMap);
            
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("category", product.getCategory().getName());
            productMap.put("imageName", product.getImageName());
            response.put("product", productMap);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching discount details: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // Extend discount duration
    @PostMapping("/discount/extend/{discountId}")
    public String extendDiscount(@PathVariable Integer discountId,
                               @RequestParam Integer additionalDays,
                               @RequestParam Integer additionalHours,
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<ProductDiscount> discountOpt = discountService.getDiscountById(discountId);
            if (discountOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Discount not found");
                return "redirect:/admin/discounts";
            }
            
            ProductDiscount discount = discountOpt.get();
            LocalDateTime newEndDate = discount.getEndDate();
            
            if (additionalDays != null && additionalDays > 0) {
                newEndDate = newEndDate.plusDays(additionalDays);
            }
            if (additionalHours != null && additionalHours > 0) {
                newEndDate = newEndDate.plusHours(additionalHours);
            }
            
            DiscountDto updateDto = new DiscountDto();
            updateDto.setEndDate(newEndDate);
            
            discountService.updateDiscount(discountId, updateDto);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Discount extended successfully! New end date: " + newEndDate);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error extending discount: " + e.getMessage());
        }
        
        return "redirect:/admin/discounts";
    }


    @GetMapping("/discounts/create/{id}")
    public String createNewDiscountForProduct(@PathVariable Integer id, Model model){
        Optional<Product> product = productService.findById(id);
        model.addAttribute("product", product);
        return "discountForm"; // new Thymeleaf template for creating discount
    }
}
