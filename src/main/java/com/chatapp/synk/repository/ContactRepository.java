package com.chatapp.synk.repository;

import com.chatapp.synk.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {
    List<Contact> findByUserIdAndContactUserId(String userId, String contactUserId);

    List<Contact> findAllByUserId(String userId);
}
