package com.example.EcommerceFresh.Controller;



import com.example.EcommerceFresh.Dao.PaymentProofDao;
import com.example.EcommerceFresh.Dao.UserOrderDao;
import com.example.EcommerceFresh.Dao.UserProfileDao;
import com.example.EcommerceFresh.Dao.OrderGroupDao;
import com.example.EcommerceFresh.Dao.DeliveryOtpDao;
import com.example.EcommerceFresh.Dao.AddressDao;
import com.example.EcommerceFresh.Entity.Category;
import com.example.EcommerceFresh.Entity.Product;
import com.example.EcommerceFresh.Entity.UserOrder;
import com.example.EcommerceFresh.Entity.OrderGroup;
import com.example.EcommerceFresh.Entity.Address;
import com.example.EcommerceFresh.Global.GlobalData;
import com.example.EcommerceFresh.Service.CategoryserviceImpl;
import com.example.EcommerceFresh.Service.ProductServiceImpl;
import com.example.EcommerceFresh.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private final PaymentProofDao paymentProofDao;
    private CategoryserviceImpl categoryservice;
    private ProductServiceImpl productService;
    private UserOrderDao userOrderDao;
    private UserProfileDao userProfileDao;
    private OrderGroupDao orderGroupDao;
    private DeliveryOtpDao deliveryOtpDao;
    private AddressDao addressDao;
    private com.example.EcommerceFresh.Service.DiscountService discountService;
    @Value("${product.images.dir:${user.dir}/productImages}")
    public String uploadDir;

    public AdminController(CategoryserviceImpl categoryservice, ProductServiceImpl productService, PaymentProofDao paymentProofDao, UserOrderDao userOrderDao, UserProfileDao userProfileDao, OrderGroupDao orderGroupDao, DeliveryOtpDao deliveryOtpDao, AddressDao addressDao, com.example.EcommerceFresh.Service.DiscountService discountService){
        this.categoryservice=categoryservice;
        this.productService=productService;
        this.paymentProofDao = paymentProofDao;
        this.userOrderDao = userOrderDao;
        this.userProfileDao = userProfileDao;
        this.orderGroupDao = orderGroupDao;
        this.deliveryOtpDao = deliveryOtpDao;
        this.addressDao = addressDao;
        this.discountService = discountService;
    }

    @GetMapping("/admin")
    public String adminHome(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "adminHome";
    }
    @GetMapping("/admin/categories")
    public String getCat(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("categories",categoryservice.getAllCategory());
        return "categories";
    }
    @GetMapping("/admin/categories/add")
    public String addCat(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
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
        model.addAttribute("cartCount", GlobalData.cart.size());
        
        // Get all products (including inactive ones) for admin view
        List<Product> products = productService.findAllProductsIncludingInactive();
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryservice.getAllCategory());
        
        // Get active discounts for products
        Map<Integer, com.example.EcommerceFresh.Entity.ProductDiscount> activeDiscounts = new java.util.HashMap<>();
        
        // Check if discount service is available and get active discounts
        try {
            if (discountService != null) {
                for (Product product : products) {
                    java.util.Optional<com.example.EcommerceFresh.Entity.ProductDiscount> discount = discountService.getActiveDiscount(product.getId());
                    if (discount.isPresent()) {
                        activeDiscounts.put(product.getId(), discount.get());
                    }
                }
            }
        } catch (Exception ex) {
            // If discount functionality isn't available, continue without it
            ex.printStackTrace();
        }
        
        model.addAttribute("activeDiscounts", activeDiscounts);
        return "products";
    }

    @GetMapping("/admin/products/add")
    public String productAdd(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
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

    @GetMapping("/admin/product/reactivate/{id}")
    public String reactivateProduct(@PathVariable int id){
        productService.reactivateProductById(id);
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
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("payments", paymentProofDao.findByStatus("Pending"));
        model.addAttribute("viewTitle", "Pending Payments");
        return "paymentManage";
    }
    @GetMapping("/admin/payment/manage/approved")
    public String approvedPaymentManager(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        model.addAttribute("payments", paymentProofDao.findByStatus("Approved"));
        model.addAttribute("viewTitle", "Approved Payments");
        return "approvedPaymentManage";
    }

    @GetMapping("/admin/payment/manage/declined")
    public String declinedPaymentManager(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
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

            // Update related user orders from Pending -> Paid
            try {
                var user = payment.getUsers();
                var product = payment.getProduct();
                if (user != null && product != null) {
                    var orders = userOrderDao.findByUserAndProductAndOrderStatus(user, product, "Pending");
                    if (orders != null) {
                        for (UserOrder ord : orders) {
                            ord.setOrderStatus("Paid");
                            userOrderDao.save(ord);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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

            // Update related user orders from Pending -> Declined
            try {
                var user = payment.getUsers();
                var product = payment.getProduct();
                if (user != null && product != null) {
                    var orders = userOrderDao.findByUserAndProductAndOrderStatus(user, product, "Pending");
                    if (orders != null) {
                        for (UserOrder ord : orders) {
                            ord.setOrderStatus("Declined");
                            userOrderDao.save(ord);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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

            // Also update related orders
            try {
                var user = payment.getUsers();
                var product = payment.getProduct();
                if (user != null && product != null) {
                    var orders = userOrderDao.findByUserAndProductAndOrderStatus(user, product, "Pending");
                    if (orders != null) {
                        for (UserOrder ord : orders) {
                            ord.setOrderStatus("Paid");
                            userOrderDao.save(ord);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return "redirect:/admin/payment/manage/declined";
    }

    @GetMapping("/admin/payment/search")
    public String searchPayments(@RequestParam(value = "q", required = false) String q, Model model){
        model.addAttribute("viewTitle","Search Results");
        if(q == null || q.trim().isEmpty()){
            model.addAttribute("payments", java.util.Collections.emptyList());
        } else {
            var results = paymentProofDao.findByTransactionIdContainingIgnoreCaseOrUsers_EmailContainingIgnoreCase(q, q);
            model.addAttribute("payments", results);
        }
        return "paymentManage"; // reuse template
    }

    @GetMapping("/admin/orders")
    public String manageOrders(@RequestParam(value = "orderId", required = false) Integer orderId,
                               @RequestParam(value = "groupId", required = false) Integer groupId,
                               Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());

        java.util.List<OrderGroup> groups;
        String info = null;
        // If groupId provided, show that specific group
        if (groupId != null) {
            java.util.Optional<OrderGroup> g = orderGroupDao.findById(groupId);
            groups = g.map(java.util.Collections::singletonList).orElse(java.util.Collections.emptyList());
            if (groups.isEmpty()) {
                info = "No order group found with id: " + groupId;
            }
        }
        // If orderId provided, show the group that contains that order (if any)
        else if (orderId != null) {
            try {
                var uoOpt = userOrderDao.findById(orderId);
                if (uoOpt.isPresent()) {
                    OrderGroup og = uoOpt.get().getOrderGroup();
                    groups = og != null ? java.util.Collections.singletonList(og) : java.util.Collections.emptyList();
                    if (groups.isEmpty()) {
                        info = "No order group found containing order id: " + orderId;
                    }
                } else {
                    groups = java.util.Collections.emptyList();
                    info = "No order found with id: " + orderId;
                }
            } catch (Exception ex) {
                groups = java.util.Collections.emptyList();
                info = "Error searching for order id: " + orderId;
            }
        }
        // default: show active groups (exclude Delivered and Cancelled)
        else {
            groups = orderGroupDao.findAll().stream()
                    .filter(og -> og.getGroupStatus() == null || (!og.getGroupStatus().equalsIgnoreCase("Delivered") && !og.getGroupStatus().equalsIgnoreCase("Cancelled")))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Ensure each group's userOrders and checkout fields are populated so templates can render full details
        for (OrderGroup og : groups) {
            // if userOrders missing, try to fetch them explicitly
            if (og.getUserOrders() == null || og.getUserOrders().isEmpty()) {
                try {
                    var orders = userOrderDao.findAll().stream()
                            .filter(uo -> uo.getOrderGroup() != null && uo.getOrderGroup().getId() != null && uo.getOrderGroup().getId().equals(og.getId()))
                            .collect(java.util.stream.Collectors.toList());
                    if (!orders.isEmpty()) og.setUserOrders(orders);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // Populate checkout fields from first UserOrder if missing
            boolean needsSave = false;
            if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                UserOrder firstOrder = og.getUserOrders().get(0);
                
                if ((og.getFirstName() == null || og.getFirstName().isBlank()) && firstOrder.getFirstName() != null) {
                    og.setFirstName(firstOrder.getFirstName());
                    needsSave = true;
                }
                if ((og.getLastName() == null || og.getLastName().isBlank()) && firstOrder.getLastName() != null) {
                    og.setLastName(firstOrder.getLastName());
                    needsSave = true;
                }
                if ((og.getMobile() == null || og.getMobile().isBlank()) && firstOrder.getMobile() != null) {
                    og.setMobile(firstOrder.getMobile());
                    needsSave = true;
                }
                if ((og.getPinCode() == null || og.getPinCode().isBlank()) && firstOrder.getPinCode() != null) {
                    og.setPinCode(firstOrder.getPinCode());
                    needsSave = true;
                }
                if ((og.getEmailAddress() == null || og.getEmailAddress().isBlank()) && firstOrder.getUser() != null && firstOrder.getUser().getEmail() != null) {
                    og.setEmailAddress(firstOrder.getUser().getEmail());
                    needsSave = true;
                }
                if ((og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) && firstOrder.getDeliveryAddress() != null) {
                    og.setDeliveryAddress(firstOrder.getDeliveryAddress());
                    needsSave = true;
                }
            }

            // if deliveryAddress is still blank, try to pick from user's saved Address
            if (og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) {
                if (og.getUser() != null && og.getUser().getEmail() != null) {
                    try {
                        var addrOpt = addressDao.findAll().stream()
                                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(og.getUser().getEmail()))
                                .findFirst();
                        if (addrOpt.isPresent()) {
                            Address addr = addrOpt.get();
                            String composed = String.format("%s, %s, %s - %s", addr.getAddress1() == null ? "" : addr.getAddress1(), addr.getTown() == null ? "" : addr.getTown(), addr.getPinCode() == null ? "" : addr.getPinCode(), addr.getPhone() == null ? "" : addr.getPhone());
                            og.setDeliveryAddress(composed);
                            needsSave = true;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (needsSave) {
                try { orderGroupDao.save(og); } catch (Exception ignored) { }
            }
        }

        model.addAttribute("orderGroups", groups);
        if (info != null) model.addAttribute("info", info);

        // compute orders that have active OTPs so template can label Resend OTP
        java.util.Set<Integer> ordersWithActiveOtp = deliveryOtpDao.findAll().stream()
                .filter(d -> !d.isUsed())
                .filter(d -> d.getExpiresAt() == null || d.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .map(d -> d.getOrder() != null ? d.getOrder().getId() : null)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("ordersWithActiveOtp", ordersWithActiveOtp);
        return "adminOrders";
    }

    @GetMapping("/admin/orders/delivered")
    public String deliveredOrders(Model model){
        model.addAttribute("cartCount", GlobalData.cart.size());
        java.util.List<OrderGroup> delivered = orderGroupDao.findAll().stream()
                .filter(og -> og.getGroupStatus() != null && og.getGroupStatus().equalsIgnoreCase("Delivered"))
                .collect(java.util.stream.Collectors.toList());

        // Ensure deliveryAddress is populated for display in admin delivered list. If missing on the group,
        // try to pick the first non-empty deliveryAddress from the group's UserOrders or from saved Address entity and persist it.
        java.util.Map<Integer, String> groupPhoneMap = new java.util.HashMap<>();
        java.util.Map<Integer, java.time.LocalDateTime> groupDeliveredAtMap = new java.util.HashMap<>();

        for (OrderGroup og : delivered) {
            // Populate checkout fields from first UserOrder if missing
            boolean needsSave = false;
            if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                UserOrder firstOrder = og.getUserOrders().get(0);
                
                if ((og.getFirstName() == null || og.getFirstName().isBlank()) && firstOrder.getFirstName() != null) {
                    og.setFirstName(firstOrder.getFirstName());
                    needsSave = true;
                }
                if ((og.getLastName() == null || og.getLastName().isBlank()) && firstOrder.getLastName() != null) {
                    og.setLastName(firstOrder.getLastName());
                    needsSave = true;
                }
                if ((og.getMobile() == null || og.getMobile().isBlank()) && firstOrder.getMobile() != null) {
                    og.setMobile(firstOrder.getMobile());
                    needsSave = true;
                }
                if ((og.getPinCode() == null || og.getPinCode().isBlank()) && firstOrder.getPinCode() != null) {
                    og.setPinCode(firstOrder.getPinCode());
                    needsSave = true;
                }
                if ((og.getEmailAddress() == null || og.getEmailAddress().isBlank()) && firstOrder.getUser() != null && firstOrder.getUser().getEmail() != null) {
                    og.setEmailAddress(firstOrder.getUser().getEmail());
                    needsSave = true;
                }
                if ((og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) && firstOrder.getDeliveryAddress() != null) {
                    og.setDeliveryAddress(firstOrder.getDeliveryAddress());
                    needsSave = true;
                }
            }
            
            if (og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) {
                // try per-order deliveryAddress
                if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                    for (UserOrder uo : og.getUserOrders()) {
                        String da = uo.getDeliveryAddress();
                        if (da != null && !da.isBlank()) {
                            og.setDeliveryAddress(da);
                            try { orderGroupDao.save(og); } catch (Exception ignored) { }
                            break;
                        }
                    }
                }
                // fallback: try Address entity for the user
                if ((og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) && og.getUser() != null && og.getUser().getEmail() != null) {
                    try {
                        var addrOpt = addressDao.findAll().stream()
                                .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(og.getUser().getEmail()))
                                .findFirst();
                        if (addrOpt.isPresent()) {
                            Address addr = addrOpt.get();
                            String composed = String.format("%s, %s, %s - %s", addr.getAddress1() == null ? "" : addr.getAddress1(), addr.getTown() == null ? "" : addr.getTown(), addr.getPinCode() == null ? "" : addr.getPinCode(), addr.getPhone() == null ? "" : addr.getPhone());
                            og.setDeliveryAddress(composed);
                            needsSave = true;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (needsSave) {
                try { orderGroupDao.save(og); } catch (Exception ignored) { }
            }

            // compute phone for this group: try UserProfile -> Address -> user's email placeholder
            String phone = null;
            try {
                if (og.getUser() != null) {
                    var upOpt = userProfileDao.findByUser(og.getUser());
                    if (upOpt.isPresent() && upOpt.get().getPhone() != null && !upOpt.get().getPhone().isBlank()) phone = upOpt.get().getPhone();
                }
            } catch (Exception ignored) { }
            if ((phone == null || phone.isBlank()) && og.getUser() != null && og.getUser().getEmail() != null) {
                try {
                    var addrOpt = addressDao.findAll().stream()
                            .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(og.getUser().getEmail()))
                            .findFirst();
                    if (addrOpt.isPresent()) phone = addrOpt.get().getPhone();
                } catch (Exception ignored) { }
            }
            if (phone == null) phone = "-";
            groupPhoneMap.put(og.getId(), phone);

            // compute deliveredAt for group as the latest deliveredAt among its orders
            java.time.LocalDateTime latest = null;
            try {
                if (og.getUserOrders() != null) {
                    for (UserOrder uo : og.getUserOrders()) {
                        if (uo.getDeliveredAt() != null) {
                            if (latest == null || uo.getDeliveredAt().isAfter(latest)) latest = uo.getDeliveredAt();
                        }
                    }
                }
            } catch (Exception ignored) { }
            if (latest != null) groupDeliveredAtMap.put(og.getId(), latest);
        }

        model.addAttribute("orderGroups", delivered);
        model.addAttribute("groupPhoneMap", groupPhoneMap);
        model.addAttribute("groupDeliveredAtMap", groupDeliveredAtMap);
        return "adminOrdersDelivered";
    }

    // Migration method to fix existing orders that are missing checkout data
    @GetMapping("/admin/migrate-checkout-data")
    @PreAuthorize("hasRole('ADMIN')")
    public String migrateCheckoutData(Model model) {
        int updatedGroups = 0;
        int updatedOrders = 0;
        
        try {
            // Get all order groups
            java.util.List<OrderGroup> allGroups = orderGroupDao.findAll();
            
            for (OrderGroup og : allGroups) {
                boolean groupNeedsSave = false;
                boolean ordersNeedSave = false;
                
                // Ensure userOrders are loaded
                if (og.getUserOrders() == null || og.getUserOrders().isEmpty()) {
                    try {
                        var orders = userOrderDao.findAll().stream()
                                .filter(uo -> uo.getOrderGroup() != null && uo.getOrderGroup().getId() != null && uo.getOrderGroup().getId().equals(og.getId()))
                                .collect(java.util.stream.Collectors.toList());
                        if (!orders.isEmpty()) {
                            og.setUserOrders(orders);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                    UserOrder firstOrder = og.getUserOrders().get(0);
                    
                    // Update OrderGroup fields if missing
                    if ((og.getFirstName() == null || og.getFirstName().isBlank()) && firstOrder.getFirstName() != null) {
                        og.setFirstName(firstOrder.getFirstName());
                        groupNeedsSave = true;
                    }
                    if ((og.getLastName() == null || og.getLastName().isBlank()) && firstOrder.getLastName() != null) {
                        og.setLastName(firstOrder.getLastName());
                        groupNeedsSave = true;
                    }
                    if ((og.getMobile() == null || og.getMobile().isBlank()) && firstOrder.getMobile() != null) {
                        og.setMobile(firstOrder.getMobile());
                        groupNeedsSave = true;
                    }
                    if ((og.getPinCode() == null || og.getPinCode().isBlank()) && firstOrder.getPinCode() != null) {
                        og.setPinCode(firstOrder.getPinCode());
                        groupNeedsSave = true;
                    }
                    if ((og.getEmailAddress() == null || og.getEmailAddress().isBlank()) && firstOrder.getUser() != null && firstOrder.getUser().getEmail() != null) {
                        og.setEmailAddress(firstOrder.getUser().getEmail());
                        groupNeedsSave = true;
                    }
                    if ((og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) && firstOrder.getDeliveryAddress() != null) {
                        og.setDeliveryAddress(firstOrder.getDeliveryAddress());
                        groupNeedsSave = true;
                    }
                    
                    // If OrderGroup fields are still missing, try to get from User or saved Address
                    if (og.getFirstName() == null || og.getFirstName().isBlank() || 
                        og.getMobile() == null || og.getMobile().isBlank() ||
                        og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank()) {
                        
                        // Try to get data from saved Address
                        if (og.getUser() != null && og.getUser().getEmail() != null) {
                            try {
                                var addrOpt = addressDao.findAll().stream()
                                        .filter(a -> a.getEmail() != null && a.getEmail().equalsIgnoreCase(og.getUser().getEmail()))
                                        .findFirst();
                                if (addrOpt.isPresent()) {
                                    Address addr = addrOpt.get();
                                    
                                    if ((og.getFirstName() == null || og.getFirstName().isBlank()) && addr.getFirstName() != null) {
                                        og.setFirstName(addr.getFirstName());
                                        groupNeedsSave = true;
                                    }
                                    if ((og.getLastName() == null || og.getLastName().isBlank()) && addr.getLastName() != null) {
                                        og.setLastName(addr.getLastName());
                                        groupNeedsSave = true;
                                    }
                                    if ((og.getMobile() == null || og.getMobile().isBlank()) && addr.getPhone() != null) {
                                        og.setMobile(addr.getPhone());
                                        groupNeedsSave = true;
                                    }
                                    if ((og.getPinCode() == null || og.getPinCode().isBlank()) && addr.getPinCode() != null) {
                                        og.setPinCode(addr.getPinCode());
                                        groupNeedsSave = true;
                                    }
                                    if ((og.getDeliveryAddress() == null || og.getDeliveryAddress().isBlank())) {
                                        String composed = String.format("%s, %s, %s - %s", 
                                            addr.getAddress1() == null ? "" : addr.getAddress1(), 
                                            addr.getTown() == null ? "" : addr.getTown(), 
                                            addr.getPinCode() == null ? "" : addr.getPinCode(), 
                                            addr.getPhone() == null ? "" : addr.getPhone());
                                        og.setDeliveryAddress(composed);
                                        groupNeedsSave = true;
                                    }
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    
                    // Update individual UserOrder fields if missing
                    for (UserOrder uo : og.getUserOrders()) {
                        boolean orderNeedsSave = false;
                        
                        if ((uo.getFirstName() == null || uo.getFirstName().isBlank()) && og.getFirstName() != null) {
                            uo.setFirstName(og.getFirstName());
                            orderNeedsSave = true;
                        }
                        if ((uo.getLastName() == null || uo.getLastName().isBlank()) && og.getLastName() != null) {
                            uo.setLastName(og.getLastName());
                            orderNeedsSave = true;
                        }
                        if ((uo.getMobile() == null || uo.getMobile().isBlank()) && og.getMobile() != null) {
                            uo.setMobile(og.getMobile());
                            orderNeedsSave = true;
                        }
                        if ((uo.getPinCode() == null || uo.getPinCode().isBlank()) && og.getPinCode() != null) {
                            uo.setPinCode(og.getPinCode());
                            orderNeedsSave = true;
                        }
                        if ((uo.getEmailAddress() == null || uo.getEmailAddress().isBlank()) && og.getEmailAddress() != null) {
                            uo.setEmailAddress(og.getEmailAddress());
                            orderNeedsSave = true;
                        }
                        if ((uo.getDeliveryAddress() == null || uo.getDeliveryAddress().isBlank()) && og.getDeliveryAddress() != null) {
                            uo.setDeliveryAddress(og.getDeliveryAddress());
                            orderNeedsSave = true;
                        }
                        
                        if (orderNeedsSave) {
                            try {
                                userOrderDao.save(uo);
                                ordersNeedSave = true;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
                
                if (groupNeedsSave) {
                    try {
                        orderGroupDao.save(og);
                        updatedGroups++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (ordersNeedSave) {
                    updatedOrders++;
                }
            }
            
            model.addAttribute("message", String.format("Migration completed! Updated %d order groups and %d individual orders.", updatedGroups, updatedOrders));
            
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("error", "Migration failed: " + ex.getMessage());
        }
        
        model.addAttribute("cartCount", GlobalData.cart.size());
        return "redirect:/admin/orders?migration=completed&updated=" + updatedGroups;
    }

    // Debug method to check current data state
    @GetMapping("/admin/debug-checkout-data")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> debugCheckoutData() {
        java.util.Map<String, Object> debug = new java.util.HashMap<>();
        
        try {
            java.util.List<OrderGroup> groups = orderGroupDao.findAll().stream()
                    .limit(10) // Just check first 10 for debugging
                    .collect(java.util.stream.Collectors.toList());
            
            java.util.List<java.util.Map<String, Object>> groupData = new java.util.ArrayList<>();
            
            for (OrderGroup og : groups) {
                java.util.Map<String, Object> groupInfo = new java.util.HashMap<>();
                groupInfo.put("id", og.getId());
                groupInfo.put("groupFirstName", og.getFirstName());
                groupInfo.put("groupLastName", og.getLastName());
                groupInfo.put("groupMobile", og.getMobile());
                groupInfo.put("groupPinCode", og.getPinCode());
                groupInfo.put("groupEmailAddress", og.getEmailAddress());
                groupInfo.put("groupDeliveryAddress", og.getDeliveryAddress());
                
                if (og.getUser() != null) {
                    groupInfo.put("userFirstName", og.getUser().getFirstName());
                    groupInfo.put("userLastName", og.getUser().getLastName());
                    groupInfo.put("userEmail", og.getUser().getEmail());
                }
                
                // Check first UserOrder
                if (og.getUserOrders() == null || og.getUserOrders().isEmpty()) {
                    try {
                        var orders = userOrderDao.findAll().stream()
                                .filter(uo -> uo.getOrderGroup() != null && uo.getOrderGroup().getId() != null && uo.getOrderGroup().getId().equals(og.getId()))
                                .collect(java.util.stream.Collectors.toList());
                        if (!orders.isEmpty()) {
                            og.setUserOrders(orders);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                
                if (og.getUserOrders() != null && !og.getUserOrders().isEmpty()) {
                    UserOrder firstOrder = og.getUserOrders().get(0);
                    groupInfo.put("orderFirstName", firstOrder.getFirstName());
                    groupInfo.put("orderLastName", firstOrder.getLastName());
                    groupInfo.put("orderMobile", firstOrder.getMobile());
                    groupInfo.put("orderPinCode", firstOrder.getPinCode());
                    groupInfo.put("orderEmailAddress", firstOrder.getEmailAddress());
                    groupInfo.put("orderDeliveryAddress", firstOrder.getDeliveryAddress());
                }
                
                groupData.add(groupInfo);
            }
            
            debug.put("orderGroups", groupData);
            debug.put("totalGroups", orderGroupDao.findAll().size());
            
        } catch (Exception ex) {
            debug.put("error", ex.getMessage());
            ex.printStackTrace();
        }
        
        return ResponseEntity.ok(debug);
    }
}
