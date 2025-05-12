package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CartController {
    @Autowired
    ProductServiceImpl productService;

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
        model.addAttribute("total",GlobalData.cart.stream().mapToDouble(Product::getPrice).sum());
        model.addAttribute("cart",GlobalData.cart);
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
