package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.CategoryserviceImpl;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {
    CategoryserviceImpl categoryservice;
    ProductServiceImpl productService;

    public HomeController(CategoryserviceImpl categoryservice,ProductServiceImpl productService){
        this.categoryservice=categoryservice;
        this.productService=productService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "redirect:/shop";
    }
    @GetMapping("/shop")
    public String shop(Model model){
        model.addAttribute("cartCount",GlobalData.cart.size());
        model.addAttribute("categories",categoryservice.getAllCategory());
        model.addAttribute("products",productService.findAllProduct());

        return "shop";
    }

    @GetMapping("/shop/category/{id}")
    public String shopByCategory(Model model, @PathVariable int id){
        model.addAttribute("cartCount",GlobalData.cart.size());
        model.addAttribute("categories",categoryservice.getAllCategory());
        model.addAttribute("products",productService.getProductByCategoryId(id));
        return "shop";
    }
    @GetMapping("/shop/viewproduct/{id}")
    public String viewProduct(Model model,@PathVariable int id){
        model.addAttribute("cartCount",GlobalData.cart.size());
        model.addAttribute("product",productService.findById(id).get());
        return "viewProduct";
    }

    @GetMapping("/home/removeItem/{id}")
    public String removeItem(@PathVariable int index){
        GlobalData.cart.remove(index);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model){
        if(GlobalData.cart.isEmpty()){
            return "redirect:/cart?error=CartIsEmpty";
        }
        model.addAttribute("total",GlobalData.cart.stream().mapToDouble(Product::getPrice).sum());
        return "checkout";
    }

}
