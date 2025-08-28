package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.RatingDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.Rating;
import com.example.EcommerceFresh.Entity.Users;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.CategoryserviceImpl;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    CategoryserviceImpl categoryservice;
    ProductServiceImpl productService;
    UserDao userDao;
    RatingDao ratingDao;

    public HomeController(CategoryserviceImpl categoryservice,ProductServiceImpl productService, UserDao userDao, RatingDao ratingDao){
        this.categoryservice=categoryservice;
        this.productService=productService;
        this.userDao = userDao;
        this.ratingDao = ratingDao;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "redirect:/shop";
    }
    @GetMapping("/shop")
    public String shop(Model model, @RequestParam(value = "sort", required = false, defaultValue = "") String sort){
        model.addAttribute("cartCount",GlobalData.cart.size());
        model.addAttribute("categories",categoryservice.getAllCategory());
        List<Product> products = productService.findAllProduct();

        // compute average ratings for each product
        Map<Integer, Double> avgRatings = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> productService.getAverageRating(p)));

        // apply sorting
        if("price_asc".equals(sort)){
            products.sort(Comparator.comparingDouble(Product::getPrice));
        } else if("price_desc".equals(sort)){
            products.sort(Comparator.comparingDouble(Product::getPrice).reversed());
        } else if("rating".equals(sort)){
            products.sort(Comparator.comparingDouble((Product p) -> avgRatings.getOrDefault(p.getId(), 0.0)).reversed());
        }

        model.addAttribute("products",products);
        model.addAttribute("avgRatings", avgRatings);
        model.addAttribute("sort", sort);

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
    public String viewProduct(Model model,@PathVariable int id, @RequestParam(value = "sort", required = false, defaultValue = "recent") String sort){
        model.addAttribute("cartCount",GlobalData.cart.size());
        var product = productService.findById(id).get();
        model.addAttribute("product",product);
        double avg = productService.getAverageRating(product);
        model.addAttribute("averageRating", avg);
        if("top".equals(sort)){
            model.addAttribute("ratings", productService.getRatingsForProductSortedByScore(product));
        } else {
            model.addAttribute("ratings", productService.getRatingsForProduct(product));
        }
        model.addAttribute("sort", sort);
        return "viewProduct";
    }

    @PostMapping("/product/{id}/rate")
    public String rateProduct(@PathVariable int id, @RequestParam("score") int score, @RequestParam(value = "comment", required = false) String comment, Principal principal){
        var product = productService.findById(id).orElseThrow();
        var userEmail = principal.getName();
        // find user by email
        var user = userDao.findByEmail(userEmail).orElseThrow();
        // check existing
        var existing = ratingDao.findByProductAndUsers(product, user);
        if(existing.isPresent()){
            var r = existing.get();
            r.setScore(score);
            r.setComment(comment);
            productService.saveRating(r);
        } else {
            Rating r = new Rating();
            r.setProduct(product);
            r.setUsers(user);
            r.setScore(score);
            r.setComment(comment);
            productService.saveRating(r);
        }
        return "redirect:/shop/viewproduct/"+id;
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
