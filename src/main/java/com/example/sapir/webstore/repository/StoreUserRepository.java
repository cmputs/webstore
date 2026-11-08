package com.example.sapir.webstore.repository;

import com.example.sapir.webstore.entity.StoreUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreUserRepository extends JpaRepository<StoreUser, Long> {
    Optional<StoreUser> findByUsername(String username);
}
