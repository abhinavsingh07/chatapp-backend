package com.chatapp.synk.controller;

import com.chatapp.synk.dto.ConversationParticipantDTO;
import com.chatapp.synk.security.JwtAuthFilter;
import com.chatapp.synk.service.ConversationParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConversationParticipantController.class, excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)})
@AutoConfigureMockMvc(addFilters = false)
@Import(ConversationParticipantControllerTest.TestConfig.class)
public class ConversationParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConversationParticipantService participantService;

    private ConversationParticipantDTO sampleParticipant;

    @BeforeEach
    public void setup() {
        Mockito.reset(participantService);
        sampleParticipant = new ConversationParticipantDTO("456_PART", "123_CONVO", "user123");
    }

    @Test
    public void testAddParticipant() throws Exception {
        when(participantService.addParticipant(any(ConversationParticipantDTO.class))).thenReturn(sampleParticipant);
        String payload = """
                    {
                        "conversationId": "123_CONVO",
                        "userId": "user123"
                    }
                """;

        mockMvc.perform(post("/api/participants").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("201")).andExpect(jsonPath("$.data[0].id").value("456_PART"));
    }

    @Test
    public void testGetParticipantById_found() throws Exception {
        when(participantService.getParticipantById("456_PART")).thenReturn(sampleParticipant);

        mockMvc.perform(get("/api/participants/456_PART")).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("200")).andExpect(jsonPath("$.data[0].id").value("456_PART"));
    }

    @Test
    public void testGetParticipantById_notFound() throws Exception {
        when(participantService.getParticipantById("456_PART")).thenReturn(null);

        mockMvc.perform(get("/api/participants/456_PART")).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("404")).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testGetByConversation_found() throws Exception {
        when(participantService.getParticipantsByConversationId("123_CONVO")).thenReturn(List.of(sampleParticipant));

        mockMvc.perform(get("/api/participants/conversation/123_CONVO")).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("200")).andExpect(jsonPath("$.data[0].conversationId").value("123_CONVO"));
    }

    @Test
    public void testGetByConversation_notFound() throws Exception {
        when(participantService.getParticipantsByConversationId("123_CONVO")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/participants/conversation/123_CONVO")).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("404")).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testDeleteParticipant() throws Exception {
        doNothing().when(participantService).deleteByConversationid("123_CONVO");

        mockMvc.perform(delete("/api/participants/conversation/123_CONVO")).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("200")).andExpect(jsonPath("$.message").value("Participant removed"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(name = "conversationParticipantService")
        @Primary
        public ConversationParticipantService participantService() {
            return mock(ConversationParticipantService.class);
        }
    }
}