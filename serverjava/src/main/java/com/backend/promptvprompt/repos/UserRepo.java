package com.backend.promptvprompt.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.promptvprompt.models.User;

@Repository
public interface UserRepo extends JpaRepository<User, String> {
    // Find user by email (for login)
    Optional<User> findByEmail(String email);

    // Check if email already exists (for registration)
    boolean existsByEmail(String email);

    // Find by verification token (for email verification)
    Optional<User> findByVerificationToken(String verificationToken);

    // Find by email and verified status
    Optional<User> findByEmailAndIsEmailVerified(String email, Boolean isEmailVerified);
}
