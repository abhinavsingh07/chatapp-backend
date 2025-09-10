package com.chatapp.synk.repository;

import com.chatapp.synk.dto.ContactUserDTO;
import com.chatapp.synk.entity.Contact;
import com.chatapp.synk.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {
    List<Contact> findByUserIdAndContactUserId(String userId, String contactUserId);

    List<Contact> findAllByUserId(String userId);

    List<Contact> findByEmailAndContactUserIdIsNull(String email);

    @Query("""
    SELECT new com.chatapp.synk.dto.ContactUserDTO(
        c.id,
        c.contactStatus,
        c.emailStatus,
        c.contactUserId,
        c.email,
        u.name,
        u.phoneNumber,
        u.email,
        u.profilePictureUrl,
        u.status,
        CASE WHEN EXISTS (
            SELECT 1 FROM Contact c2
            WHERE c2.userId = c.contactUserId
              AND c2.contactUserId = c.userId
              AND c2.contactStatus = com.chatapp.synk.enums.ContactStatus.ADDED
        ) THEN true ELSE false END,
        c.userId
    )
    FROM Contact c
    LEFT JOIN User u ON c.contactUserId = u.id
    WHERE c.userId = :userId
    """)
    List<ContactUserDTO> findContactUserDetailsByUserId(@Param("userId") String userId);

    @Query("SELECT new com.chatapp.synk.dto.ContactUserDTO(" + "c.id, c.contactStatus, c.emailStatus, c.contactUserId, c.email, " + "u.name, u.phoneNumber, u.email, u.profilePictureUrl, u.status,c.userId) " + "FROM Contact c JOIN User u ON c.contactUserId = u.id")
    List<ContactUserDTO> findAllContactsWithUserDetails();

    @Modifying
    @Query("UPDATE Contact c SET c.contactUserId = :userId,c.contactStatus=:contactStatus WHERE c.email = :email AND c.contactUserId IS NULL")
    int updateContactUserIdByEmail(@Param("userId") String userId, @Param("contactStatus") ContactStatus contactStatus, @Param("email") String email);

    boolean existsByUserIdAndContactUserIdAndContactStatus(String userId, String contactUserId, ContactStatus contactStatus);
}
