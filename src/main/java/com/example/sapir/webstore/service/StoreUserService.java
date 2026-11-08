package com.example.sapir.webstore.service;

import com.example.sapir.webstore.dto.RegisterRequest;
import com.example.sapir.webstore.entity.StoreUser;
import com.example.sapir.webstore.repository.StoreUserRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StoreUserService {

    private final StoreUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public StoreUserService(StoreUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {
        String password = request.password();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password exceeds the BCrypt limit of 72 bytes");
        }

        StoreUser user = new StoreUser(request.username().strip(), request.email().strip());
        user.setPasswordHash(passwordEncoder.encode(password));
        try {
            repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Could not create account with these details", ex);
        }
    }
}
