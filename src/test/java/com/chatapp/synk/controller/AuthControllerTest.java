package com.chatapp.synk.controller;

import com.chatapp.synk.dto.AuthDTO;
import com.chatapp.synk.dto.UserDTO;
import com.chatapp.synk.repository.UserRepository;
import com.chatapp.synk.security.CustomUserDetails;
import com.chatapp.synk.security.CustomUserDetailsService;
import com.chatapp.synk.service.UserService;
import com.chatapp.synk.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters
@Import(AuthControllerTest.TestConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationManager authenticationManager;  // This is now injected from TestConfig

    @Autowired
    private JwtUtil jwtUtil;  // This is now injected from TestConfig
    @Autowired
    private UserRepository userRepository; // This is now injected from TestConfig
    @Autowired
    private CustomUserDetailsService userDetailsService;  // This is now injected from TestConfig

    @Autowired
    private UserService userService;  // This is now injected from TestConfig
    private UserDTO sampleUser;

    @BeforeEach
    void setUp() {
        Mockito.reset(authenticationManager, jwtUtil, userDetailsService, userService);
        sampleUser = new UserDTO("8dc2c03d-b35a-4b9a-a212-b1d4a20dc56a_USER", "9999999999", "abc@xyz.com", "password", "Abhinav", "https://example.com/pic.jpg", "Backend Dev");
    }

    @Test
    void testAuthenticate() throws Exception {
        String phoneNumber = "1234567890";
        String name = "testuser";
        String email = "testemail";
        String profilePictureUrl = "testprofilePictureUrl";
        String password = "testpassword";
        String userRole= "ROLE_USER";
        String mockToken= "mockToken";

        // Create a sample AuthDTO request
        AuthDTO request = new AuthDTO();
        request.setPhoneNumberOrEmail(phoneNumber);
        request.setPassword(password);

        CustomUserDetails mockUserDetails = new CustomUserDetails(phoneNumber, name, email, userRole, profilePictureUrl);

        when(userDetailsService.loadUserByUsername(request.getPhoneNumberOrEmail())).thenReturn(mockUserDetails);
        //when(jwtUtil.generateToken(mockUserDetails)).thenReturn(mockToken);

        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumberOrEmail\": \"" + phoneNumber + "\"}"))
                .andExpect(status().isOk())
                //.andExpect(jsonPath("$.expiry").value(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))) to do
                .andExpect(jsonPath("$.jwtToken").value(mockToken))
                .andExpect(jsonPath("$.username").value(phoneNumber))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.roles").value(userRole))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.profilePictureUrl").value(profilePictureUrl));

    }

    @Test
    public void testCreateUser() throws Exception {
        when(userService.createUser(any(UserDTO.class))).thenReturn(sampleUser);

        String jsonInput = """
                {
                  "phoneNumber": "9999999999",
                  "name": "Abhinav",
                  "email":"abc@xyz.com",
                  "password":"hello",
                  "profilePictureUrl": "https://example.com/pic.jpg",
                  "about": "Backend Dev"
                }
                """;

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(jsonInput)).andExpect(status().isOk()).andExpect(jsonPath("$.responseCode").value("200")).andExpect(jsonPath("$.message").value("User created successfully")).andExpect(jsonPath("$.data[0].name").value("Abhinav"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean(name = "authenticationManager")
        @Primary
        public AuthenticationManager authenticationManager() {
            return mock(AuthenticationManager.class);
        }

        @Bean(name = "jwtUtil")
        @Primary
        public JwtUtil jwtUtil() {
            return mock(JwtUtil.class);
        }

        @Bean(name = "userRepository")
        @Primary
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean(name = "userDetailsService")
        @Primary
        public CustomUserDetailsService userDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean(name = "userService")
        @Primary
        public UserService userService() {
            return mock(UserService.class);
        }
    }
}
