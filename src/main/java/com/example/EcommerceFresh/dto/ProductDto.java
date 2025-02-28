    package com.example.EcommerceFresh.dto;


    import com.example.EcommerceFresh.Entity.Category;
    import jakarta.persistence.*;
    import lombok.Data;


    @Data
    @Table(name="product")
    public class ProductDto {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @Column(name="product_id")
        private int id;

        @Column(name="product_name")
        private String name;
        private int category;
        @Column(name="price")
        private double price;
        @Column(name="weight")
        private double weight;
        @Column(name="description")
        private String description;
        @Column(name="image_name")
        private String imageName;
    }
