package com.example.demo.repository;

import com.example.demo.entity.Church;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurchRepository extends JpaRepository<Church, UUID> {

    Optional<Church> findByName(String name);

    boolean existsByName(String name);
}
