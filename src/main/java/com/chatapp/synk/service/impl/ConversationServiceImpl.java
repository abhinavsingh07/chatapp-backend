package com.chatapp.synk.service.impl;

import com.chatapp.synk.dto.ConversationDTO;
import com.chatapp.synk.entity.Conversation;
import com.chatapp.synk.entity.ConversationParticipant;
import com.chatapp.synk.enums.ConversationType;
import com.chatapp.synk.repository.ConversationParticipantRepository;
import com.chatapp.synk.repository.ConversationRepository;
import com.chatapp.synk.service.ConversationService;
import com.chatapp.synk.util.Mapper;
import com.chatapp.synk.util.RandomUUIDGenerater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Override
    @Caching(put = {@CachePut(value = "conversationCache", key = "#result.id", unless = "#result == null")},
            evict = {@CacheEvict(value = "conversationCache", key = "'allConversations'")})
    public ConversationDTO createConversation(ConversationDTO dto) {
        logger.info("Creating new conversation");
        Conversation entity = Mapper.mapToConversationEntity(dto);
        Conversation saved = conversationRepository.save(entity);
        logger.info("Conversation saved with ID: {}", saved.getId());
        return Mapper.mapToConversationDTO(saved);
    }

    @Override
    @Cacheable(value = "conversationCache", key = "#id", unless = "#result == null")
    public ConversationDTO getConversationById(String id) {
        logger.info("Fetching conversation with ID: {}", id);
        Optional<ConversationDTO> result = conversationRepository.findById(id.trim()).map(Mapper::mapToConversationDTO);
        if (result.isEmpty()) {
            logger.warn("No conversation found with ID: {}", id);
            return null;
        }
        return result.get();
    }

    @Override
    @Cacheable(value = "conversationCache", key = "'allConversations'")
    public List<ConversationDTO> findAll() {
        logger.info("fetching all conversationIds");
        return conversationRepository.findAll().stream().map(Mapper::mapToConversationDTO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "conversationCache", key = "#loggedInUserId + '_' + #contactUserId", unless = "#result == null")
    @Transactional // ensures conversation + participants are saved atomically (rollback if anything fails).
    public String getOrCreateConversation(String loggedInUserId, String contactUserId) {
        logger.info("Request to get or create conversation between [{}] and [{}]", loggedInUserId, contactUserId);

        // Try to find existing conversation
        String conversationId = conversationRepository.findConversationIdByUserIdAndContactUserId(loggedInUserId.trim(), contactUserId.trim());

        if (conversationId != null) {
            logger.info("Existing conversation [{}] found between [{}] and [{}]", conversationId, loggedInUserId, contactUserId);
            return conversationId; // Reuse existing
        }

        // Create new conversation
        String newConversationId = RandomUUIDGenerater.getId(Conversation.ALIAS_CONVERSATION).toString();
        Conversation conversation = new Conversation(newConversationId, ConversationType.ONE_TO_ONE.toString());
        conversationRepository.save(conversation);
        logger.info("Created new conversation [{}] between [{}] and [{}]", newConversationId, loggedInUserId, contactUserId);

        // Add both participants
        List<ConversationParticipant> participants = List.of(
                new ConversationParticipant(RandomUUIDGenerater.getId(ConversationParticipant.ALIAS_PARTICIPANT).toString(), newConversationId, loggedInUserId.trim()),
                new ConversationParticipant(RandomUUIDGenerater.getId(ConversationParticipant.ALIAS_PARTICIPANT).toString(), newConversationId, contactUserId.trim()));

        participantRepository.saveAll(participants);
        logger.info("Added participants [{}] and [{}] to conversation [{}]", loggedInUserId, contactUserId, newConversationId);

        return newConversationId;
    }
}
