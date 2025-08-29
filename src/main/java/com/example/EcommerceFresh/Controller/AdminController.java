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
    public String uploadDir = System.getProperty("user.dir") + "/productImages";

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
        Optional<Category> temp=categoryservice.findCategoryById(id);
        if(temp.isPresent()){
            model.addAttribute("category",temp.get());
            return "categoriesAdd";
        }else
            return "error";
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
                                     @RequestParam(value = "imgName", required = false) String imgName)throws IOException {


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

        // Preserve existing image name if updating
        String existingImageName = null;
        if (productDto.getId() != 0) {
            try {
                var existing = productService.findById(productDto.getId());
                if (existing.isPresent()) {
                    existingImageName = existing.get().getImageName();
                }
            } catch (Exception ex) {
                // ignore
                ex.printStackTrace();
            }
        }

        if(!file.isEmpty()){
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            imageUUID = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID() + ext;

            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path fileNameAndPath= Paths.get(uploadDir,imageUUID);
            Files.write(fileNameAndPath,file.getBytes());

            // Delete old image file if present and different from default
            try {
                if (existingImageName != null && !existingImageName.equals("default.png") && !existingImageName.equals(imageUUID)) {
                    Path old = Paths.get(uploadDir, existingImageName);
                    if (Files.exists(old)) {
                        Files.delete(old);
                    }
                }
            } catch (Exception ex) {
                // best-effort
                ex.printStackTrace();
            }
        }else{
            // if no new file uploaded, prefer imgName (from form) -> existingImageName -> default
            imageUUID = imgName != null && !imgName.isEmpty() ? imgName : (existingImageName != null ? existingImageName : "default.png");
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

    @GetMapping("/admin/payment/manage/declined")
    public String declinedPaymentManager(Model model){
        model.addAttribute("payments", paymentProofDao.findByStatus("Declined"));
        model.addAttribute("viewTitle", "Declined Payments");
        return "declinedPaymentManage";
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

    @GetMapping("/admin/payment/decline/{id}")
    public String declinePayment(@PathVariable int id){
        var paymentOpt=paymentProofDao.findById(id);
        if(paymentOpt.isPresent()){
            var payment=paymentOpt.get();
            payment.setStatus("Declined");
            paymentProofDao.save(payment);
        }
        return "redirect:/admin/payment/manage/pending";
    }

    @GetMapping("/admin/payment/view/{id}")
    public String viewPayment(@PathVariable int id, Model model){
        var paymentOpt = paymentProofDao.findById(id);
        if(paymentOpt.isPresent()){
            model.addAttribute("payment", paymentOpt.get());
            return "paymentView";
        }
        return "redirect:/admin/payment/manage/pending";
    }

    @GetMapping("/admin/payment/re-approve/{id}")
    public String reApproveDeclined(@PathVariable int id){
        var paymentOpt=paymentProofDao.findById(id);
        if(paymentOpt.isPresent()){
            var payment=paymentOpt.get();
            payment.setStatus("Approved");
            paymentProofDao.save(payment);
        }
        return "redirect:/admin/payment/manage/declined";
    }
}
