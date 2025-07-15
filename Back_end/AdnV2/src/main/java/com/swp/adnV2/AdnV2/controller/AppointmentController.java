package com.swp.adnV2.AdnV2.controller;

import com.swp.adnV2.AdnV2.dto.AppointmentRequest;
import com.swp.adnV2.AdnV2.dto.AppointmentResponse;
import com.swp.adnV2.AdnV2.dto.AppointmentUpdateRequest;
import com.swp.adnV2.AdnV2.entity.Appointment;
import com.swp.adnV2.AdnV2.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Validator;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolation;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/create/guest-appointment/{serviceId}")
    public ResponseEntity<?> createGuestAppointment(
            @PathVariable("serviceId") Long serviceId,
            @Valid @RequestBody AppointmentRequest request) {
        return appointmentService.createGuestAppointment(serviceId, request);
    }

    @DeleteMapping("/delete-appointment/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<?> deleteAppointment(@PathVariable("id") Long appointmentId) {
        return appointmentService.deleteAppointment(appointmentId);
    }

    @GetMapping("/get-all-appointments")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    /**
     * Tạo cuộc hẹn mới (cho cả người dùng đăng ký và khách vãng lai)
     */
    @PostMapping("/create-appointment/{serviceId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> createAppointment(
            @PathVariable("serviceId") Long serviceId,
            @RequestBody AppointmentRequest request,
            BindingResult bindingResult) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Validate dữ liệu sau khi đã xác thực
        jakarta.validation.Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory()
                .getValidator();
        java.util.Set<jakarta.validation.ConstraintViolation<AppointmentRequest>> violations = validator
                .validate(request);
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String username = authentication.getName();
        return appointmentService.createAppointment(serviceId, request, username);
    }

    /**
     * Xem danh sách cuộc hẹn của người dùng đăng ký
     */
    @GetMapping("/view-appointments-user")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> viewUserAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String username = authentication.getName();
        return appointmentService.viewAppointments(username);
    }

    /**
     * Xem thông tin chi tiết của một cuộc hẹn
     */
    @GetMapping("/view-appointment/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getAppointmentById(
            @PathVariable("id") Long appointmentId) {
        return appointmentService.getAppointmentById(appointmentId);
    }

    /**
     * Cập nhật trạng thái của cuộc hẹn
     */
    @PutMapping("update/staff/{appointmentId}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<?> updateAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestBody AppointmentUpdateRequest updateRequest) {
        return appointmentService.updateAppointment(appointmentId, updateRequest);
    }

    @GetMapping("/view-appointment-guest")
    public ResponseEntity<?> findGuestAppointments(
            @RequestParam String email,
            @RequestParam String phone) {
        return appointmentService.findAppointmentsByEmailAndPhone(email, phone);
    }

    @GetMapping("/get/appointment-by-status")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getAppointmentsByUsernameAndStatus(
            @RequestParam(required = false) String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String username = authentication.getName();

        return ResponseEntity.ok(appointmentService.getAppointmentByUsernameAndStatus(username, status));
    }

}
