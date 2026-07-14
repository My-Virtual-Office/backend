package com.virtualoffice.service.user.service;

import com.virtualoffice.service.user.domain.entity.User;
import com.virtualoffice.service.user.domain.entity.VerificationRequest;
import com.virtualoffice.service.user.domain.enumuration.VerificationRequestStatus;
import com.virtualoffice.service.user.domain.enumuration.VerificationRequestType;
import com.virtualoffice.service.user.dto.ApiResponse;
import com.virtualoffice.service.user.repository.VerificationRequestRepository;
import com.virtualoffice.service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final NotificationService notificationService;
    private final VerificationRequestRepository verificationRequestRepository;
    private final PasswordEncoder passwordEncoder; // for encrypting the otp
    private final UserRepository userRepository;

    public ApiResponse requestOtp(String email, VerificationRequestType type) {
        User user = userRepository.findByEmail(email)
                .orElse(null);
        if (user == null){
            return new ApiResponse("User not found");
        }


        generateAndSendOtp(user, type);
        return new ApiResponse("OTP generated");
    }

    public ApiResponse verifyOtp(String email, String plainOtpFromUser, VerificationRequestType type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user found with this email"));

        VerificationRequest request = verificationRequestRepository
                .getOtpByUserAndType(
                        user.getId(), type, VerificationRequestStatus.PENDING)
                .orElse(null);

        if (request == null){
            return new ApiResponse("No Pending OTP found");
        }

        if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        // check if the OTP is the corret one
        boolean isValid = passwordEncoder.matches(plainOtpFromUser, request.getOtp());

        if (!isValid) {
            verificationRequestRepository.save(request);
            return new ApiResponse("Invalid OTP");
        }

        request.setStatus(VerificationRequestStatus.APPROVED);
        verificationRequestRepository.save(request);

        return new ApiResponse("OTP verified");
    }

    // Complete a forgotten-password flow: re-validate the emailed OTP, then set the new password.
    // Kept as a single call so a password can only be changed by someone holding the fresh OTP.
    public ApiResponse resetPassword(String email, String plainOtpFromUser, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return new ApiResponse("Password must be at least 6 characters");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ApiResponse("User not found");
        }

        VerificationRequest request = verificationRequestRepository
                .getOtpByUserAndType(user.getId(), VerificationRequestType.PASSWORD_RESET,
                        VerificationRequestStatus.PENDING)
                .orElse(null);
        if (request == null) {
            return new ApiResponse("No Pending OTP found");
        }
        if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new ApiResponse("OTP expired");
        }
        if (!passwordEncoder.matches(plainOtpFromUser, request.getOtp())) {
            return new ApiResponse("Invalid OTP");
        }

        // OTP checks out — set the new (hashed) password and consume the request.
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        request.setStatus(VerificationRequestStatus.APPROVED);
        verificationRequestRepository.save(request);

        return new ApiResponse("Password reset");
    }

    public void generateAndSendOtp(User user, VerificationRequestType type) {

        // get a random OTP
        String plainOtp = generateOtp();

        // save the hashed version in db
        String hashedOtp = passwordEncoder.encode(plainOtp);

        VerificationRequest request = VerificationRequest.builder()
                .otp(hashedOtp)
                .status(VerificationRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .type(type)
                .user(user)
                .build();

        verificationRequestRepository.save(request);

        notificationService.otpNotification(user, plainOtp);

    }

    // generate OTP
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}