package com.chatapp.synk.service.impl;

import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.entity.Message;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.MessageRepository;
import com.chatapp.synk.service.MessageService;
import com.chatapp.synk.util.Mapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public List<MessageDTO> getMessagesByConversationId(String conversationId) {
        logger.info("Fetching messages for conversation: {}", conversationId);
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId.trim())
                .stream().map(Mapper::mapToMessageDTO).collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getUnreadMessagesForReceiver(String conversationId, String receiverId) {
        logger.info("Fetching unread messages for receiver: {}", receiverId);
        return messageRepository.findByConversationIdAndReceiverId(conversationId.trim(),receiverId.trim())
                .stream().map(Mapper::mapToMessageDTO).collect(Collectors.toList());
    }

    @Override
    public MessageDTO createMessage(MessageDTO messageDTO) {
        logger.info("Sending message from {} to {}", messageDTO.getSenderId(), messageDTO.getReceiverId());
        Message message = Mapper.mapToMessageEntity(messageDTO);
        Message saved = messageRepository.save(message);
        return Mapper.mapToMessageDTO(saved);
    }

    @Override
    @Transactional
    public void markMessageAsRead(String messageId) {
        logger.info("Marking message as read. ID: {}", messageId);
        Message message = messageRepository.findById(messageId.trim())
                .orElseThrow(() -> new ServiceException("Message not found", HttpStatus.NOT_FOUND));
        //message.setIsRead(true);
        messageRepository.save(message);
    }
}
