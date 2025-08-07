package com.chatapp.synk.repository;

import com.chatapp.synk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findByEmailContainingIgnoreCaseOrPhoneNumberContaining(String namePart, String phonePart);
}