package com.ymd.wifi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Customer customer;

    private long amountTsh;
    private String method;
    @Column(unique = true)
    private String reference;
    private LocalDateTime paidAt = LocalDateTime.now();

    public Payment() {}
    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public long getAmountTsh() { return amountTsh; }
    public void setAmountTsh(long amountTsh) { this.amountTsh = amountTsh; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
