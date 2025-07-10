package com.swp.adnV2.AdnV2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp.adnV2.AdnV2.dto.AppointmentRequest;
import com.swp.adnV2.AdnV2.dto.AppointmentUpdateRequest;
import com.swp.adnV2.AdnV2.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AppointmentController appointmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController).build();
        objectMapper = new ObjectMapper();

        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateAppointmentSuccess() throws Exception {
        // Arrange
        AppointmentRequest request = new AppointmentRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("0123456789");
        request.setAppointmentDate(LocalDateTime.now().plusDays(1));
        request.setDob(LocalDate.of(1990, 1, 1));
        request.setGender("Male");
        request.setTestPurpose("Civil");
        request.setServiceType("DNA Testing");
        request.setTestCategory("Paternity");

        when(authentication.getName()).thenReturn("testuser");
        when(appointmentService.createAppointment(eq(1L), any(AppointmentRequest.class), eq("testuser")))
                .thenReturn(ResponseEntity.ok().build());

        // Act & Assert
        mockMvc.perform(post("/api/create-appointment/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateAppointmentWithoutAuthentication() throws Exception {
        // Arrange
        AppointmentRequest request = new AppointmentRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("0123456789");

        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/create-appointment/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateAppointmentWithInvalidData() throws Exception {
        // Arrange
        AppointmentRequest request = new AppointmentRequest();
        // Missing required fields
        request.setFullName("");
        request.setEmail("invalid-email");
        request.setPhone("");

        when(authentication.getName()).thenReturn("testuser");

        // Act & Assert
        mockMvc.perform(post("/api/create-appointment/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testViewUserAppointmentsSuccess() throws Exception {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(appointmentService.viewAppointments("testuser"))
                .thenReturn(ResponseEntity.ok().build());

        // Act & Assert
        mockMvc.perform(get("/api/view-appointments-user"))
                .andExpect(status().isOk());
    }

    @Test
    void testViewUserAppointmentsWithoutAuthentication() throws Exception {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/view-appointments-user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAppointmentByIdSuccess() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById(1L))
                .thenReturn(ResponseEntity.ok().build());

        // Act & Assert
        mockMvc.perform(get("/api/view-appointment/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAppointmentByIdNotFound() throws Exception {
        // Arrange
        when(appointmentService.getAppointmentById(999L))
                .thenReturn(ResponseEntity.notFound().build());

        // Act & Assert
        mockMvc.perform(get("/api/view-appointment/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateAppointmentSuccess() throws Exception {
        // Arrange
        AppointmentUpdateRequest updateRequest = new AppointmentUpdateRequest();
        updateRequest.setStatus("CONFIRMED");

        when(appointmentService.updateAppointment(eq(1L), any(AppointmentUpdateRequest.class)))
                .thenReturn(ResponseEntity.ok().build());

        // Act & Assert
        mockMvc.perform(put("/api/update/staff/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteAppointmentSuccess() throws Exception {
        // Arrange
        when(appointmentService.deleteAppointment(1L))
                .thenReturn(ResponseEntity.ok().build());

        // Act & Assert
        mockMvc.perform(delete("/api/delete-appointment/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAppointmentsByStatusSuccess() throws Exception {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(appointmentService.getAppointmentByUsernameAndStatus(eq("testuser"), anyString()))
                .thenReturn(java.util.Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/get/appointment-by-status")
                .param("status", "PENDING"))
                .andExpect(status().isOk());
    }
}