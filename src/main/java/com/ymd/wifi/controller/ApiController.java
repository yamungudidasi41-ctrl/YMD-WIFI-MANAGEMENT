package com.ymd.wifi.controller;

import com.ymd.wifi.model.*;
import com.ymd.wifi.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final CustomerRepository customers;
    private final PackageRepository packages;
    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;

    public ApiController(CustomerRepository customers, PackageRepository packages,
                         SubscriptionRepository subscriptions, PaymentRepository payments) {
        this.customers = customers; this.packages = packages;
        this.subscriptions = subscriptions; this.payments = payments;
    }

    private boolean loggedIn(HttpSession s) { return s.getAttribute("adminId") != null; }
    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please log in"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        long active = subscriptions.findAll().stream()
            .filter(x -> x.getExpiryDate() != null && !x.getExpiryDate().isBefore(LocalDate.now())).count();
        long expired = subscriptions.findAll().stream()
            .filter(x -> x.getExpiryDate() != null && x.getExpiryDate().isBefore(LocalDate.now())).count();
        long revenue = payments.findAll().stream().mapToLong(Payment::getAmountTsh).sum();
        return ResponseEntity.ok(Map.of("customers", customers.count(), "packages", packages.count(),
                "activeSubscriptions", active, "expiredSubscriptions", expired, "totalRevenueTsh", revenue));
    }

    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers(HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        return ResponseEntity.ok(customers.findAll());
    }

    @PostMapping("/customers")
    public ResponseEntity<?> addCustomer(@Valid @RequestBody Customer c, HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        if (customers.findByPhone(c.getPhone()).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number already exists"));
        return ResponseEntity.ok(customers.save(c));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer c, HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        return customers.findById(id).map(old -> {
            old.setFullName(c.getFullName()); old.setPhone(c.getPhone()); old.setAddress(c.getAddress());
            return ResponseEntity.ok(customers.save(old));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id, HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        if (!customers.existsById(id)) return ResponseEntity.notFound().build();
        try { customers.deleteById(id); return ResponseEntity.noContent().build(); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "Customer has related records and cannot be deleted")); }
    }

    @GetMapping("/packages")
    public ResponseEntity<?> getPackages(HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        return ResponseEntity.ok(packages.findAll());
    }

    @PostMapping("/packages")
    public ResponseEntity<?> addPackage(@Valid @RequestBody WifiPackage p, HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        return ResponseEntity.ok(packages.save(p));
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<?> addSubscription(@RequestBody Map<String, Object> body, HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        Long customerId = Long.valueOf(body.get("customerId").toString());
        Long packageId = Long.valueOf(body.get("packageId").toString());
        var customer = customers.findById(customerId);
        var pack = packages.findById(packageId);
        if (customer.isEmpty() || pack.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Customer or package not found"));
        LocalDate start = LocalDate.parse(body.getOrDefault("startDate", LocalDate.now().toString()).toString());
        Subscription sub = new Subscription();
        sub.setCustomer(customer.get()); sub.setWifiPackage(pack.get());
        sub.setStartDate(start); sub.setExpiryDate(start.plusDays(pack.get().getDurationDays()));
        sub.setStatus("ACTIVE");
        return ResponseEntity.ok(subscriptions.save(sub));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<?> getSubscriptions(HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        return ResponseEntity.ok(subscriptions.findAllByOrderByExpiryDateAsc());
    }

    @PostMapping("/payments")
    public ResponseEntity<?> addPayment(@RequestBody Map<String, Object> body, HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        Long customerId = Long.valueOf(body.get("customerId").toString());
        long amount = Long.parseLong(body.get("amountTsh").toString());
        if (amount <= 0) return ResponseEntity.badRequest().body(Map.of("error", "Amount must be greater than zero"));
        var customer = customers.findById(customerId);
        if (customer.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Customer not found"));
        Payment p = new Payment();
        p.setCustomer(customer.get()); p.setAmountTsh(amount);
        p.setMethod(body.getOrDefault("method", "CASH").toString());
        p.setReference(body.getOrDefault("reference", "").toString());
        return ResponseEntity.ok(payments.save(p));
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getPayments(HttpSession s) {
        if (!loggedIn(s)) return unauthorized();
        return ResponseEntity.ok(payments.findAllByOrderByPaidAtDesc());
    }
}
