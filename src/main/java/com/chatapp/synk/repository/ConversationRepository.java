package com.chatapp.synk.repository;

import com.chatapp.synk.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("SELECT conv.id " +
            "FROM Conversation conv " +
            "JOIN ConversationParticipant cp1 ON conv.id = cp1.conversationId " +
            "JOIN ConversationParticipant cp2 ON conv.id = cp2.conversationId " +
            "WHERE cp1.userId = :userId " +
            "  AND cp2.userId = :contactUserId " +
            "  AND conv.conversationType = 'one_to_one'")
     String findConversationIdByUserIdAndContactUserId(@Param("userId") String userId, @Param("contactUserId") String contactUserId);

}