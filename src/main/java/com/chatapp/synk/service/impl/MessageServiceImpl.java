package com.chatapp.synk.service.impl;

import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.entity.Message;
import com.chatapp.synk.exceptionHandler.ServiceException;
import com.chatapp.synk.repository.MessageRepository;
import com.chatapp.synk.service.MessageService;
import com.chatapp.synk.util.Mapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
    private final MessageRepository messageRepository;

    public MessageServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public List<MessageDTO> getMessagesByConversationId(String conversationId) {
        String validId = InputSecurityUtils.secureId(conversationId);
        return messageRepository.findByConversationIdOrderBySentAtAsc(validId)
                .stream()
                .map(Mapper::mapToMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getUnreadMessagesForReceiver(String conversationId, String receiverId) {
        String receiverValidId = InputSecurityUtils.secureId(receiverId);
        String conversationValidId = InputSecurityUtils.secureId(conversationId);
        return messageRepository.findByConversationIdAndReceiverId(conversationValidId, receiverValidId)
                .stream()
                .map(Mapper::mapToMessageDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MessageDTO saveMessage(MessageDTO messageDTO) {
        MessageDTO validDTO = InputValidationAndSanitizationService.validateAndSanitize(messageDTO);
        logger.info("Saving message from {} to {}", validDTO.getSenderId(), validDTO.getReceiverId());
        Message message = Mapper.mapToMessageEntity(validDTO);
        Message saved = messageRepository.save(message);
        logger.info("Message saved with ID: {}", saved.getId());
        return Mapper.mapToMessageDTO(saved);
    }

    @Override
    @Transactional
    public void markMessageAsRead(String messageId) {
        String validId = InputSecurityUtils.secureId(messageId);
        Message message = messageRepository.findById(validId)
                .orElseThrow(() -> {
                    logger.warn("Message not found with ID: {}", validId);
                    return new ServiceException("Message not found", HttpStatus.NOT_FOUND);
                });
        //message.setIsRead(true);
        messageRepository.save(message);
        logger.info("Message marked as read. ID: {}", validId);
    }
}

