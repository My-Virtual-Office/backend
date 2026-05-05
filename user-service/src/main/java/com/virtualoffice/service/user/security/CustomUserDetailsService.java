package com.virtualoffice.service.user.security;

import com.virtualoffice.service.user.domain.entity.User;
import com.virtualoffice.service.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email
                ));

        if (user.isDisabled()) {
            throw new UsernameNotFoundException("User account is disabled: " + email);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .accountExpired(false)
                .accountLocked(user.getAccountStatus() ==
                        com.virtualoffice.service.user.domain.enumuration.AccountStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(user.isDisabled())
                .build();
    }
}