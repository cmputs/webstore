package com.example.sapir.webstore.repository;

import com.example.sapir.webstore.entity.StoreUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreUserRepository extends JpaRepository<StoreUser, Long> {
}
