package com.chatapp.synk.repository;

import com.chatapp.synk.dto.ChatListDTO;
import com.chatapp.synk.entity.ConversationLastMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ConversationLastMessageRepository extends JpaRepository<ConversationLastMessage, String> {
    @Modifying   // tells Spring this query changes data
    @Transactional // ensures atomic commit/rollback
    @Query(value = "INSERT INTO conversation_last_message " + "(conversation_id, message_id, sender_id, content) " +
            "VALUES (:conversationId, :messageId, :senderId, :content) " +
            "ON DUPLICATE KEY UPDATE " +
            "message_id = VALUES(message_id), " +
            "sender_id = VALUES(sender_id), " +
            "content = VALUES(content)", nativeQuery = true)
    void upsertLastMessage(@Param("conversationId") String conversationId,
                           @Param("messageId") String messageId,
                           @Param("senderId") String senderId,
                           @Param("content") String content);

    @Query(value = """
            SELECT clm.id as lastMessageId,
                   clm.conversation_id as conversationId,
                   clm.content as content,
                   clm.sent_at as sentAt,
                   clm.sender_id as senderId,
                   u.id as participantId,
                   u.name as participantName,
                   u.profile_picture_url as participantProfilePic
            FROM conversation_participants cp_self
            INNER JOIN conversation_last_message clm
                   ON cp_self.conversation_id = clm.conversation_id
            INNER JOIN conversation_participants cp_other
                   ON clm.conversation_id = cp_other.conversation_id
            INNER JOIN users u
                   ON cp_other.user_id = u.id
            WHERE cp_self.user_id = :loggedInUserId
              AND cp_other.user_id != :loggedInUserId
            ORDER BY clm.sent_at DESC
            """, nativeQuery = true)
    List<ChatListDTO> findUserConversations(@Param("loggedInUserId") String loggedInUserId);

}
