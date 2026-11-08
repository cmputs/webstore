package com.example.sapir.webstore.service;

import com.example.sapir.webstore.entity.StoreUser;
import com.example.sapir.webstore.repository.StoreUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StoreUserDetailsService implements UserDetailsService {

    private final StoreUserRepository repository;

    public StoreUserDetailsService(StoreUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        StoreUser storeUser = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        String passwordHash = storeUser.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        return User.withUsername(storeUser.getUsername())
                .password(passwordHash)
                .roles(storeUser.getRole().name())
                .build();
    }
}
