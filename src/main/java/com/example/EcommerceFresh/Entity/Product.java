package com.example.EcommerceFresh.Entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY  )
    @Column(name="product_id")
    private int id;

    @Column(name="product_name")
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="category_id" , referencedColumnName = "category_id")
    private Category category;
    @Column(name="price")
    private double price;
    @Column(name="weight")
    private double weight;
    @Column(name="description")
    private String description;
    @Column(name="image_name")
    private String imageName;
}
