package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.RatingDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.WishlistDao;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.ProductDiscount;
import com.example.EcommerceFresh.Entity.Rating;
import com.example.EcommerceFresh.Entity.Users;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.CategoryserviceImpl;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import com.example.EcommerceFresh.Service.DiscountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    CategoryserviceImpl categoryservice;
    ProductServiceImpl productService;
    UserDao userDao;
    RatingDao ratingDao;
    WishlistDao wishlistDao;
    DiscountService discountService;

    public HomeController(CategoryserviceImpl categoryservice,ProductServiceImpl productService, UserDao userDao, RatingDao ratingDao, WishlistDao wishlistDao, DiscountService discountService){
        this.categoryservice=categoryservice;
        this.productService=productService;
        this.userDao = userDao;
        this.ratingDao = ratingDao;
        this.wishlistDao = wishlistDao;
        this.discountService = discountService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "redirect:/shop";
    }
    @GetMapping("/shop")
    public String shop(Model model, @RequestParam(value = "sort", required = false, defaultValue = "") String sort, Principal principal){
        model.addAttribute("cartCount",GlobalData.cart.size());
        model.addAttribute("categories",categoryservice.getAllCategory());
        List<Product> products = productService.findAllProduct();

        // compute average ratings for each product
        Map<Integer, Double> avgRatings = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> productService.getAverageRating(p)));

        // compute rounded ratings (integers) to use in templates without Math.round
        Map<Integer, Integer> roundedRatings = avgRatings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (int) Math.round(e.getValue())));

        // compute discount information for each product
        Map<Integer, ProductDiscount> activeDiscounts = new java.util.HashMap<>();
        Map<Integer, Double> effectivePrices = new java.util.HashMap<>();
        
        for (Product product : products) {
            Optional<ProductDiscount> discount = discountService.getActiveDiscount(product.getId());
            if (discount.isPresent()) {
                activeDiscounts.put(product.getId(), discount.get());
            }
            effectivePrices.put(product.getId(), discountService.getEffectivePrice(product));
        }

        // apply sorting
        if("price_asc".equals(sort)){
            products.sort(Comparator.comparingDouble(p -> effectivePrices.get(p.getId())));
        } else if("price_desc".equals(sort)){
            products.sort(Comparator.comparingDouble((Product p) -> effectivePrices.get(p.getId())).reversed());
        } else if("rating".equals(sort)){
            products.sort(Comparator.comparingDouble((Product p) -> avgRatings.getOrDefault(p.getId(), 0.0)).reversed());
        }

        model.addAttribute("products",products);
        model.addAttribute("avgRatings", avgRatings);
        model.addAttribute("roundedRatings", roundedRatings);
        model.addAttribute("activeDiscounts", activeDiscounts);
        model.addAttribute("effectivePrices", effectivePrices);
        model.addAttribute("sort", sort);
        // add user's wishlist product ids if authenticated
        java.util.Set<Integer> wishlistIds = java.util.Collections.emptySet();
        if(principal != null){
            var userOpt = userDao.findByEmail(principal.getName());
            if(userOpt.isPresent()){
                var user = userOpt.get();
                java.util.List<com.example.EcommerceFresh.Entity.Wishlist> wl = wishlistDao.findByUser(user);
                wishlistIds = wl.stream().map(w -> w.getProduct() != null ? w.getProduct().getId() : null).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            }
        }
        model.addAttribute("wishlistIds", wishlistIds);

        return "shop";
    }

    @GetMapping("/shop/category/{id}")
    public String shopByCategory(Model model, @PathVariable int id, @RequestParam(value = "sort", required = false, defaultValue = "") String sort, Principal principal){
        model.addAttribute("cartCount",GlobalData.cart.size());
        model.addAttribute("categories",categoryservice.getAllCategory());
        List<Product> products = productService.getProductByCategoryId(id);
        if(products == null) products = java.util.Collections.emptyList();

        // compute average ratings for each product (defensive: skip null ids)
        Map<Integer, Double> avgRatings = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> productService.getAverageRating(p)));

        // compute rounded ratings (integers) to use in templates without Math.round
        Map<Integer, Integer> roundedRatings = avgRatings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (int) Math.round(e.getValue())));

        // compute discount information for each product
        Map<Integer, ProductDiscount> activeDiscounts = new java.util.HashMap<>();
        Map<Integer, Double> effectivePrices = new java.util.HashMap<>();
        
        for (Product product : products) {
            Optional<ProductDiscount> discount = discountService.getActiveDiscount(product.getId());
            if (discount.isPresent()) {
                activeDiscounts.put(product.getId(), discount.get());
            }
            effectivePrices.put(product.getId(), discountService.getEffectivePrice(product));
        }

        // apply sorting same as /shop
        if("price_asc".equals(sort)){
            products.sort(Comparator.comparingDouble(p -> effectivePrices.get(p.getId())));
        } else if("price_desc".equals(sort)){
            products.sort(Comparator.comparingDouble((Product p) -> effectivePrices.get(p.getId())).reversed());
        } else if("rating".equals(sort)){
            products.sort(Comparator.comparingDouble((Product p) -> avgRatings.getOrDefault(p.getId(), 0.0)).reversed());
        }

        model.addAttribute("products", products);
        model.addAttribute("avgRatings", avgRatings);
        model.addAttribute("roundedRatings", roundedRatings);
        model.addAttribute("activeDiscounts", activeDiscounts);
        model.addAttribute("effectivePrices", effectivePrices);
        model.addAttribute("sort", sort);
        // add user's wishlist product ids if authenticated
        java.util.Set<Integer> wishlistIds = java.util.Collections.emptySet();
        if(principal != null){
            var userOpt = userDao.findByEmail(principal.getName());
            if(userOpt.isPresent()){
                var user = userOpt.get();
                java.util.List<com.example.EcommerceFresh.Entity.Wishlist> wl = wishlistDao.findByUser(user);
                wishlistIds = wl.stream().map(w -> w.getProduct() != null ? w.getProduct().getId() : null).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            }
        }
        model.addAttribute("wishlistIds", wishlistIds);
        return "shop";
    }
    @GetMapping("/shop/viewproduct/{id}")
    public String viewProduct(Model model,@PathVariable int id, @RequestParam(value = "sort", required = false, defaultValue = "recent") String sort, Principal principal){
        model.addAttribute("cartCount",GlobalData.cart.size());
        var product = productService.findById(id).get();
        model.addAttribute("product",product);
        double avg = productService.getAverageRating(product);
        model.addAttribute("averageRating", avg);
        model.addAttribute("roundedAverage", (int)Math.round(avg));
        // add formatted string to avoid Thymeleaf format functions
        String formatted = String.format(Locale.US, "%.1f", avg);
        model.addAttribute("formattedAverage", formatted);
        
        // Add discount information
        Optional<ProductDiscount> activeDiscount = discountService.getActiveDiscount(product.getId());
        if (activeDiscount.isPresent()) {
            model.addAttribute("activeDiscount", activeDiscount.get());
        }
        model.addAttribute("effectivePrice", discountService.getEffectivePrice(product));
        
        if("top".equals(sort)){
            model.addAttribute("ratings", productService.getRatingsForProductSortedByScore(product));
        } else {
            model.addAttribute("ratings", productService.getRatingsForProduct(product));
        }
        model.addAttribute("sort", sort);
        // wishlist flag for this product
        java.util.Set<Integer> wishlistIds = java.util.Collections.emptySet();
        if(principal != null){
            var userOpt = userDao.findByEmail(principal.getName());
            if(userOpt.isPresent()){
                var user = userOpt.get();
                java.util.List<com.example.EcommerceFresh.Entity.Wishlist> wl = wishlistDao.findByUser(user);
                wishlistIds = wl.stream().map(w -> w.getProduct() != null ? w.getProduct().getId() : null).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            }
        }
        model.addAttribute("wishlistIds", wishlistIds);
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

    // ... checkout is handled by CheckOutController

}
