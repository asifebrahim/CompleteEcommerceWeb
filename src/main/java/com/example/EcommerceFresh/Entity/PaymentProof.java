package com.example.EcommerceFresh.Entity;

import jakarta.persistence.*;

@Entity
public class PaymentProof {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name="Transaction_id")
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "User_id", referencedColumnName = "id")
    private Users users;

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
}
