package com.swp.adnV2.AdnV2.repository;

import com.swp.adnV2.AdnV2.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AccountRepository extends JpaRepository<Users, Long> {
    // In AccountRepository.java
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Users findByUsername(String username);
}
