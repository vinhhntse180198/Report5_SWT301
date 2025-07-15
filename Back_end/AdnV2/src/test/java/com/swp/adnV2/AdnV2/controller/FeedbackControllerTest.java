package com.swp.adnV2.AdnV2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp.adnV2.AdnV2.dto.FeedbackRequest;
import com.swp.adnV2.AdnV2.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedbackControllerTest {

        @Mock
        private FeedbackService feedbackService;

        @Mock
        private SecurityContext securityContext;

        @Mock
        private Authentication authentication;

        @InjectMocks
        private FeedbackController feedbackController;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(feedbackController).build();
                objectMapper = new ObjectMapper();

                // Setup security context
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);
        }

        @Test
        void testCreateFeedbackSuccess() throws Exception {
                // Arrange
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(5);
                request.setContent("Great service!");

                when(authentication.getName()).thenReturn("testuser");
                when(feedbackService.createFeedback(eq("testuser"), eq(1L), any(FeedbackRequest.class)))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(post("/api/feedback/create/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void testCreateFeedbackWithoutAuthentication() throws Exception {
                // Arrange
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(5);
                request.setContent("Great service!");

                when(securityContext.getAuthentication()).thenReturn(null);

                // Act & Assert
                mockMvc.perform(post("/api/feedback/create/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testCreateFeedbackWithEmptyComment() throws Exception {
                // Arrange
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(5);
                request.setContent(""); // Empty content

                when(authentication.getName()).thenReturn("testuser");

                // Act & Assert
                mockMvc.perform(post("/api/feedback/create/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testSearchFeedbacksByServiceNameSuccess() throws Exception {
                // Arrange
                when(feedbackService.searchFeedback(eq("DNA Testing"), anyString()))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(get("/api/feedback/search/by-service-name/DNA Testing")
                                .param("keyword", "good"))
                                .andExpect(status().isOk());
        }

        @Test
        void testSearchFeedbacksByServiceNameWithoutKeyword() throws Exception {
                // Arrange
                when(feedbackService.searchFeedback(eq("DNA Testing"), isNull()))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(get("/api/feedback/search/by-service-name/DNA Testing"))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateFeedbackSuccess() throws Exception {
                // Arrange
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(4);
                request.setContent("Updated comment");

                when(authentication.getName()).thenReturn("testuser");
                when(feedbackService.updateFeedback(eq("testuser"), eq(1L), any(FeedbackRequest.class)))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(put("/api/feedback/update/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void testUpdateFeedbackWithoutAuthentication() throws Exception {
                // Arrange
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(4);
                request.setContent("Updated comment");

                when(securityContext.getAuthentication()).thenReturn(null);

                // Act & Assert
                mockMvc.perform(put("/api/feedback/update/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testDeleteFeedbackSuccess() throws Exception {
                // Arrange
                when(feedbackService.deleteFeedback(1L))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(delete("/api/feedback/delete/1"))
                                .andExpect(status().isOk());
        }

        @Test
        void testDeleteFeedbackNotFound() throws Exception {
                // Arrange
                when(feedbackService.deleteFeedback(999L))
                                .thenReturn(ResponseEntity.notFound().build());

                // Act & Assert
                mockMvc.perform(delete("/api/feedback/delete/999"))
                                .andExpect(status().isNotFound());
        }

        // --- CREATE FEEDBACK TESTS ---
        @Test
        void testCreateFeedbackWithUnauthorizedRole() throws Exception {
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(4);
                request.setContent("Nice");

                // Simulate GUEST role (not allowed)
                when(authentication.getName()).thenReturn("guestuser");
                // Simulate forbidden by not stubbing service

                mockMvc.perform(post("/api/feedback/create/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testCreateFeedbackWithInvalidServiceId() throws Exception {
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(4);
                request.setContent("Nice");

                when(authentication.getName()).thenReturn("testuser");
                when(feedbackService.createFeedback(eq("testuser"), eq(-1L), any(FeedbackRequest.class)))
                                .thenReturn(ResponseEntity.badRequest().build());

                mockMvc.perform(post("/api/feedback/create/-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // --- UPDATE FEEDBACK TESTS ---
        @Test
        void testUpdateFeedbackWithUnauthorizedRole() throws Exception {
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(3);
                request.setContent("Update");

                // Simulate GUEST role (not allowed)
                when(authentication.getName()).thenReturn("guestuser");

                mockMvc.perform(put("/api/feedback/update/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testUpdateFeedbackWithInvalidContent() throws Exception {
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(3);
                request.setContent(""); // Invalid

                when(authentication.getName()).thenReturn("testuser");

                mockMvc.perform(put("/api/feedback/update/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateFeedbackWithNonExistentId() throws Exception {
                FeedbackRequest request = new FeedbackRequest();
                request.setRating(3);
                request.setContent("Update");

                when(authentication.getName()).thenReturn("testuser");
                when(feedbackService.updateFeedback(eq("testuser"), eq(999L), any(FeedbackRequest.class)))
                                .thenReturn(ResponseEntity.notFound().build());

                mockMvc.perform(put("/api/feedback/update/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // --- DELETE FEEDBACK TESTS ---
        @Test
        void testDeleteFeedbackWithUnauthorizedRole() throws Exception {
                // Simulate CUSTOMER role (not allowed)
                when(authentication.getName()).thenReturn("customeruser");

                mockMvc.perform(delete("/api/feedback/delete/1"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testDeleteFeedbackWithMissingAuthentication() throws Exception {
                when(securityContext.getAuthentication()).thenReturn(null);

                mockMvc.perform(delete("/api/feedback/delete/1"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testDeleteFeedbackWithManagerRole() throws Exception {
                // Simulate MANAGER role
                when(authentication.getName()).thenReturn("manageruser");
                when(feedbackService.deleteFeedback(2L)).thenReturn(ResponseEntity.ok().build());

                mockMvc.perform(delete("/api/feedback/delete/2"))
                                .andExpect(status().isOk());
        }

}