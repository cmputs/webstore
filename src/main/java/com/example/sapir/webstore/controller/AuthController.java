package com.example.sapir.webstore.controller;

import com.example.sapir.webstore.dto.RegisterRequest;
import com.example.sapir.webstore.service.StoreUserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StoreUserService storeUserService;

    public AuthController(StoreUserService storeUserService) {
        this.storeUserService = storeUserService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        storeUserService.register(request);
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Principal principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        return Map.of("authenticated", true, "username", principal.getName());
    }
}
