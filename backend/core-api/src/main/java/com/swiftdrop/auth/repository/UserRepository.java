package com.swiftdrop.auth.repository;

import com.swiftdrop.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmail(String email);
    List<User> findByRoleIgnoreCase(String role);
}
