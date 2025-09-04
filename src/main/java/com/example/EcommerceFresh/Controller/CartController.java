package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import com.example.EcommerceFresh.Service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CartController {
    @Autowired
    ProductServiceImpl productService;
    
    @Autowired
    DiscountService discountService;

    @GetMapping("/addToCart/{id}")
    public String addToCart(@PathVariable int id) {
        var product = productService.findById(id);

        if (product.isPresent()) {
            GlobalData.cart.add(product.get());
        } else {
            // Redirect to an error page or display an error message
            return "redirect:/shop?error=ProductNotFound";
        }

        return "redirect:/shop";
    }

    @GetMapping("/cart")
    public String getCart(Model model){
        model.addAttribute("cartCount",GlobalData.cart.size());
        
        // Calculate total with discounts applied
        double totalWithDiscounts = GlobalData.cart.stream()
                .mapToDouble(product -> discountService.getEffectivePrice(product))
                .sum();
        
        model.addAttribute("total", totalWithDiscounts);
        model.addAttribute("cart",GlobalData.cart);
        
        // Add discount service to model for template to check discounts
        model.addAttribute("discountService", discountService);
        
        return "cart";
    }

    @GetMapping("/cart/removeItem/{index}")
    public String removeItem(@PathVariable("index") int index){
        if(index>=0 && index<GlobalData.cart.size()){
            GlobalData.cart.remove(index);
        }
        return "redirect:/cart";

    }

}
