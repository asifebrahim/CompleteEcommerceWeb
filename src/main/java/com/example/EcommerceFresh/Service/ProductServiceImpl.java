package com.example.EcommerceFresh.Service;


import com.example.EcommerceFresh.Dao.ProductDao;
import com.example.EcommerceFresh.Entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl {
    private ProductDao productDao;
    public ProductServiceImpl(ProductDao productDao){
        this.productDao=productDao;
    }

    public void Save(Product tempProduct){
        productDao.save(tempProduct);
    }
    public Optional<Product> findById(int id){
        Optional<Product> ans=productDao.findById(id);
        return ans;
    }

    public List<Product> findAllProduct(){
        return productDao.findAll();
    }

    public void removeProductById(int id){
        productDao.deleteById(id);
    }

    public List<Product> getProductByCategoryId(int id){
        return  productDao.findAllBycategoryId(id);
    }
}
