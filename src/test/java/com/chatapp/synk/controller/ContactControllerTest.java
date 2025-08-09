package com.chatapp.synk.controller;

import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.security.JwtAuthFilter;
import com.chatapp.synk.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
})
@AutoConfigureMockMvc(addFilters = false)
@Import(ContactControllerTest.TestConfig.class)
public class ContactControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactService contactService;

    private UserDTO sampleContact;

    @BeforeEach
    public void setup() {
        Mockito.reset(contactService);
        //sampleContact = new UserDTO("456_CONT", "userphone", "useremail","userpassword", "User Name", "http://example.com/profile.jpg", "About User");
    }

    @Test
    public void testGetContactById_found() throws Exception {
        //when(contactService.getContactsByUserId("456_CONT")).thenReturn(Arrays.asList(sampleContact));

        mockMvc.perform(get("/api/contacts/456_CONT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("456_CONT"));
    }

    @Test
    public void testGetContactById_notFound() throws Exception {
        when(contactService.getContactsByUserId("456_CONT")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/contacts/456_CONT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("404"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(name = "contactService")
        @Primary
        public ContactService contactService() {
            return mock(ContactService.class);
        }
    }
}