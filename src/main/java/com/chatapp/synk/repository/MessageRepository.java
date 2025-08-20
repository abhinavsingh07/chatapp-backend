package com.chatapp.synk.repository;

import com.chatapp.synk.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByConversationIdOrderBySentAtAsc(String conversationId);

    List<Message> findByConversationIdAndReceiverId(String conversationId, String receiverId);

}