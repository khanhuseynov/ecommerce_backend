package com.kahnhuseynov.ecommercebackend.user.repository;

import com.kahnhuseynov.ecommercebackend.user.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Used to find role by its name during user registration
    @EntityGraph(attributePaths = "roles")
    Optional<Role> findByName(String name);
}