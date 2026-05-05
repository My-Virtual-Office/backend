package com.virtualoffice.service.user.repository;

import com.virtualoffice.service.user.domain.entity.VerificationRequest;
import com.virtualoffice.service.user.domain.enumuration.VerificationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    Optional<VerificationRequest> findByUserIdAndStatus(Long userId, VerificationRequestStatus status);
}