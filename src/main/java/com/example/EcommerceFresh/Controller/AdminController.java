package com.example.EcommerceFresh.Controller;



import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Entity.Category;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Service.CategoryserviceImpl;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import com.example.EcommerceFresh.dto.ProductDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Controller
public class AdminController {

    private final PaymentProofDao paymentProofDao;
    public String uploadDir=System.getProperty("user.dir")+"/src/main/resources/static/productImages";

    private CategoryserviceImpl categoryservice;
    private ProductServiceImpl productService;
    public AdminController(CategoryserviceImpl categoryservice, ProductServiceImpl productService, PaymentProofDao paymentProofDao){
        this.categoryservice=categoryservice;
        this.productService=productService;
        this.paymentProofDao = paymentProofDao;
    }

    @GetMapping("/admin")
    public String adminHome(){
        return "adminHome";
    }
    @GetMapping("/admin/categories")
    public String getCat(Model model){
        model.addAttribute("categories",categoryservice.getAllCategory());
        return "categories";
    }
    @GetMapping("/admin/categories/add")
    public String addCat(Model model){
        model.addAttribute("category",new Category());
        return "categoriesAdd";
    }

    @PostMapping("/admin/categories/add")
    public String PostCatAdd(@ModelAttribute("category") Category tempCategory){
        categoryservice.Save(tempCategory);
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/delete/{id}")
    public String deleteCat(@PathVariable int id){
//        Integer catId=categoryservice.getIdByName(name);
        categoryservice.deleteCat(id);
        return "redirect:/admin/categories";

    }

    @GetMapping("/admin/categories/update/{id}")
    public String updateCat(@PathVariable int id,Model model){
        Optional temp=categoryservice.findCategoryById(id);
        if(temp!=null){
            model.addAttribute("category",temp.get());
            return "categoriesAdd";
        }else
            return "404";
    }

    @GetMapping("/admin/products")
    public String getProduct(Model model){
        model.addAttribute("products",productService.findAllProduct());
        return "products";
    }

    @GetMapping("/admin/products/add")
    public String productAdd(Model model){
        model.addAttribute("productDTO",new ProductDto());
        model.addAttribute("categories",categoryservice.getAllCategory());
        return "productsAdd";
    }
    @PostMapping("/admin/products/add")
    public String productPostProcess(@ModelAttribute("productDTO") ProductDto productDto,
                                    @RequestParam("productImage")MultipartFile file,
                                     @RequestParam("imgName") String imgName)throws IOException {


        Product product=new Product();
        product.setId(productDto.getId());
        product.setName(productDto.getName());
        Category category = categoryservice.findCategoryById(productDto.getCategory())
                .orElseThrow(() -> new RuntimeException("Category not found for id: " + productDto.getCategory()));
        product.setCategory(category);

        product.setPrice(productDto.getPrice());
        product.setWeight(productDto.getWeight());
        product.setDescription(productDto.getDescription());
        String imageUUID;
        if(!file.isEmpty()){
            imageUUID=file.getOriginalFilename();
            Path fileNameAndPath= Paths.get(uploadDir,imageUUID);
            Files.write(fileNameAndPath,file.getBytes());
        }else{
            imageUUID=imgName;
        }
        product.setImageName(imageUUID);
        productService.Save(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/product/delete/{id}")
    public String deleteProduct(@PathVariable int id){
        productService.removeProductById(id);
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/product/update/{id}")
    public String updateProduct(@PathVariable int id,Model model){
        Product product=productService.findById(id).get();
        ProductDto productDto=new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());
        productDto.setCategory(product.getCategory().getId());
        productDto.setPrice(product.getPrice());
        productDto.setWeight(product.getWeight());
        productDto.setDescription(product.getDescription());
        productDto.setImageName(product.getImageName());
        model.addAttribute("categories",categoryservice.getAllCategory());
        model.addAttribute("productDTO",productDto);
        return "productsAdd";

    }

    @GetMapping("/admin/payment/manage/pending")
    public String paymentManager(Model model){
        model.addAttribute("payments", paymentProofDao.findByStatus("Pending"));
        model.addAttribute("viewTitle", "Pending Payments");
        return "paymentManage";
    }
    @GetMapping("/admin/payment/manage/approved")
    public String approvedPaymentManager(Model model){
        model.addAttribute("payments", paymentProofDao.findByStatus("Approved"));
        model.addAttribute("viewTitle", "Approved Payments");
        return "approvedPaymentManage";
    }

    @GetMapping("/admin/payment/approve/{id}")
    public String approvePayment(@PathVariable int id){
        var paymentOpt=paymentProofDao.findById(id);
        if(paymentOpt.isPresent()){
            var payment=paymentOpt.get();
            payment.setStatus("Approved");
            paymentProofDao.save(payment);
        }
        return "redirect:/admin/payment/manage/pending";
    }
}
