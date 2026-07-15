package com.kahnhuseynov.ecommercebackend.user.repository;

import com.kahnhuseynov.ecommercebackend.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used to check if the email is already registered in the system
    boolean existsByEmail(String email);

    // Used during authentication to load user with roles
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);
}