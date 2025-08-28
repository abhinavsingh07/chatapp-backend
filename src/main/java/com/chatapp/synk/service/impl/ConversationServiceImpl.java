package com.chatapp.synk.service.impl;

import com.chatapp.synk.security_validator.InputSecurityUtils;
import com.chatapp.synk.security_validator.InputValidationAndSanitizationService;
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

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository, ConversationParticipantRepository participantRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    @Caching(put = {@CachePut(value = "conversationCache", key = "#result.id", unless = "#result == null")},
            evict = {@CacheEvict(value = "conversationCache", key = "'allConversations'", beforeInvocation = true)})
    public ConversationDTO createConversation(ConversationDTO dto) {
        ConversationDTO validTO = InputValidationAndSanitizationService.validateAndSanitize(dto);
        Conversation entity = Mapper.mapToConversationEntity(validTO);
        Conversation saved = conversationRepository.save(entity);

        logger.info("Conversation created with ID: {}", saved.getId());
        return Mapper.mapToConversationDTO(saved);
    }

    @Override
    @Cacheable(value = "conversationCache", key = "#id", unless = "#result == null")
    public ConversationDTO getConversationById(String id) {
        String validId = InputSecurityUtils.secureId(id);
        Optional<ConversationDTO> result = conversationRepository.findById(validId).map(Mapper::mapToConversationDTO);

        if (result.isEmpty()) {
            logger.warn("No conversation found with ID: {}", id);
            return null;
        }
        // Debug only when found (not spammy at scale)
        if (logger.isDebugEnabled()) {
            logger.debug("Fetched conversation with ID: {}", id);
        }
        return result.get();
    }

    @Override
    @Cacheable(value = "conversationCache", key = "'allConversations'")
    public List<ConversationDTO> findAll() {
        if (logger.isDebugEnabled()) {
            logger.debug("Fetching all conversations from DB");
        }
        return conversationRepository.findAll().stream().map(Mapper::mapToConversationDTO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "conversationCache", key = "#loggedInUserId + '_' + #contactUserId", unless = "#result == null")
    @Transactional
    public String getOrCreateConversation(String loggedInUserId, String contactUserId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Get or create conversation request between [{}] and [{}]", loggedInUserId, contactUserId);
        }

        String loggedInUserValidId = InputSecurityUtils.secureId(loggedInUserId);
        String contactUserValidId = InputSecurityUtils.secureId(contactUserId);

        String conversationId = conversationRepository.findConversationIdByUserIdAndContactUserId(
                loggedInUserValidId, contactUserValidId);

        if (conversationId != null) {
            logger.info("Existing conversation [{}] reused between [{}] and [{}]",
                    conversationId, loggedInUserValidId, contactUserValidId);
            return conversationId;
        }

        String newConversationId = RandomUUIDGenerater.getId(Conversation.ALIAS_CONVERSATION).toString();
        Conversation conversation = new Conversation(newConversationId, ConversationType.ONE_TO_ONE.toString());
        conversationRepository.save(conversation);

        logger.info("New conversation [{}] created between [{}] and [{}]",
                newConversationId, loggedInUserId, contactUserId);

        List<ConversationParticipant> participants = List.of(
                new ConversationParticipant(RandomUUIDGenerater.getId(ConversationParticipant.ALIAS_PARTICIPANT).toString(), newConversationId, loggedInUserId),
                new ConversationParticipant(RandomUUIDGenerater.getId(ConversationParticipant.ALIAS_PARTICIPANT).toString(), newConversationId, contactUserId)
        );
        participantRepository.saveAll(participants);

        logger.info("Participants [{}] and [{}] added to conversation [{}]",
                loggedInUserId, contactUserId, newConversationId);

        return newConversationId;
    }
}

