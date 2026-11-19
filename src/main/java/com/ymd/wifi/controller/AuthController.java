package com.ymd.wifi.controller;

import com.ymd.wifi.repository.AdminRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminRepository admins;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(AdminRepository admins) { this.admins = admins; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        var admin = admins.findByUsername(username);
        if (admin.isPresent() && encoder.matches(password, admin.get().getPasswordHash())) {
            session.setAttribute("adminId", admin.get().getId());
            return ResponseEntity.ok(Map.of("message", "Login successful", "username", username));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        if (session.getAttribute("adminId") == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
        return ResponseEntity.ok(Map.of("loggedIn", true));
    }
}
