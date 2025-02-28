package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name="category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="category_id")
    private int id;

    @Column(name="category_name")
    private String name;

    @OneToMany(mappedBy = "category",
            fetch = FetchType.LAZY)
    List<Product> products;

    public void add(Product tempProduct){
        if(products==null){
            products=new ArrayList<>();

        }
        products.add(tempProduct);
        tempProduct.setCategory(this);

    }

}
