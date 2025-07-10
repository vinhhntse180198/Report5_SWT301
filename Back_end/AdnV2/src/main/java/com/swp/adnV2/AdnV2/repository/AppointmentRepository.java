package com.swp.adnV2.AdnV2.repository;

import com.swp.adnV2.AdnV2.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // Tìm theo user_id
    List<Appointment> findByUsers_UserIdAndIsActiveTrue(Long userId);

    List<Appointment> findByIsActiveTrue();
    List<Appointment> findByUsers_UsernameAndIsActiveTrue(String username);

    // Tìm theo email và phone cho người không có tài khoản
    List<Appointment> findByEmailAndPhoneAndIsActiveTrue(String email, String phone);

    List<Appointment> findByUsers_UsernameAndStatus(String username, String status);

}
