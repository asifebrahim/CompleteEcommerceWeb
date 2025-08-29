package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
public class PaymentProof {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name="Transaction_id")
    @NotNull
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "User_id", referencedColumnName = "id")
    private Users users;

    @ManyToOne
    @JoinColumn(name="address_id",referencedColumnName = "id")
    private Address address;

    @ManyToOne
    @JoinColumn(name="product_id",referencedColumnName = "id")
    private Product product;

    @Column(name="status")
    private String status;

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Address getAddress() {
        return address;
    }
    public void setAddress(Address address) {
        this.address = address;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }
}
