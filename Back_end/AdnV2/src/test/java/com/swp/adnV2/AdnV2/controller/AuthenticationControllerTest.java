package com.swp.adnV2.AdnV2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp.adnV2.AdnV2.config.JwtUtil;
import com.swp.adnV2.AdnV2.dto.AuthenticationRequest;
import com.swp.adnV2.AdnV2.entity.Users;
import com.swp.adnV2.AdnV2.repository.UserRepository;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Authentication")
@Feature("Login")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationControllerTest {

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private UserDetailsService userDetailsService;

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private AuthenticationController authenticationController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
                objectMapper = new ObjectMapper();
        }

        @Test
        @TmsLink("TC011")
        @Description("Đăng nhập thành công. Ví dụ: Username=testuser; Password=password123; authorities=ROLE_CUSTOMER")
        void testLoginSuccess() throws Exception {
                // Arrange
                AuthenticationRequest request = new AuthenticationRequest();
                request.setUsername("testuser");
                request.setPassword("password123");

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                                .withUsername("testuser")
                                .password("password123")
                                .authorities("ROLE_CUSTOMER")
                                .build();

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, null,
                                                userDetails.getAuthorities()));
                when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
                when(jwtUtil.generateToken(userDetails)).thenReturn("valid-jwt-token");

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.jwt").value("valid-jwt-token"));
        }

        @Test
        @TmsLink("TC012")
        @Description("Sai mật khẩu. Ví dụ: Username=testuser; Password=wrongpassword; authorities=ROLE_CUSTOMER")
        void testLoginWrongPassword() throws Exception {
                // Arrange
                AuthenticationRequest request = new AuthenticationRequest();
                request.setUsername("testuser");
                request.setPassword("wrongpassword");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @TmsLink("TC013")
        @Description("Không tồn tại user. Ví dụ: Username=nonexistentuser; Password=password123; authorities=ROLE_CUSTOMER")
        void testLoginUserNotFound() throws Exception {
                // Arrange
                AuthenticationRequest request = new AuthenticationRequest();
                request.setUsername("nonexistentuser");
                request.setPassword("password123");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Bad credentials"));

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @TmsLink("TC014")
        @Description("Thiếu thông tin. Ví dụ: Username=''; Password=''; authorities=ROLE_CUSTOMER")
        void testLoginEmptyCredentials() throws Exception {
                // Arrange
                AuthenticationRequest request = new AuthenticationRequest();
                request.setUsername("");
                request.setPassword("");

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @TmsLink("TC015")
        @Description("Thiếu thông tin. Ví dụ: Username=null; Password=null; authorities=ROLE_CUSTOMER")
        void testLoginNullCredentials() throws Exception {
                // Arrange
                AuthenticationRequest request = new AuthenticationRequest();
                request.setUsername(null);
                request.setPassword(null);

                // Act & Assert
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }
}