package com.swp.adnV2.AdnV2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swp.adnV2.AdnV2.dto.AppointmentRequest;
import com.swp.adnV2.AdnV2.dto.AppointmentUpdateRequest;
import com.swp.adnV2.AdnV2.service.AppointmentService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.qameta.allure.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
                objectMapper.registerModule(new JavaTimeModule());
                // Setup security context
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @TmsLink("TC001")
        @Description("Kiểm tra tạo appointment thành công với dữ liệu hợp lệ")
        @Severity(SeverityLevel.CRITICAL)
        void testCreateAppointmentSuccess() throws Exception {
                Allure.step("Tạo request với dữ liệu hợp lệ");
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

                Allure.step("Gửi request POST /api/create-appointment/1");
                mockMvc.perform(post("/api/create-appointment/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
                Allure.step("Kiểm tra response trả về 200 OK");
        }

        @Test
        @TmsLink("TC002")
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
        @TmsLink("TC003")
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
        @TmsLink("TC004")
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
        @TmsLink("TC005")
        void testViewUserAppointmentsWithoutAuthentication() throws Exception {
                // Arrange
                when(securityContext.getAuthentication()).thenReturn(null);

                // Act & Assert
                mockMvc.perform(get("/api/view-appointments-user"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @TmsLink("TC006")
        void testGetAppointmentByIdSuccess() throws Exception {
                // Arrange
                when(appointmentService.getAppointmentById(1L))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(get("/api/view-appointment/1"))
                                .andExpect(status().isOk());
        }

        @Test
        @TmsLink("TC007")
        void testGetAppointmentByIdNotFound() throws Exception {
                // Arrange
                when(appointmentService.getAppointmentById(999L))
                                .thenReturn(ResponseEntity.notFound().build());

                // Act & Assert
                mockMvc.perform(get("/api/view-appointment/999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @TmsLink("TC008")
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
        @TmsLink("TC009")
        void testDeleteAppointmentSuccess() throws Exception {
                // Arrange
                when(appointmentService.deleteAppointment(1L))
                                .thenReturn(ResponseEntity.ok().build());

                // Act & Assert
                mockMvc.perform(delete("/api/delete-appointment/1"))
                                .andExpect(status().isOk());
        }

        @Test
        @TmsLink("TC010")
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