package com.prodwatch.api.service;

import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.repository.ProfileRepository;

import org.springframework.stereotype.Service;

@Service
public class SignupEmailService {

    static final String EMAIL_TAKEN_MESSAGE = "An account with this email already exists";

    private final ProfileRepository profileRepository;
    private final SupabaseAuthService supabaseAuthService;

    public SignupEmailService(ProfileRepository profileRepository, SupabaseAuthService supabaseAuthService) {
        this.profileRepository = profileRepository;
        this.supabaseAuthService = supabaseAuthService;
    }

    public boolean isEmailAvailable(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) {
            return false;
        }
        if (profileRepository.existsByEmailIgnoreCase(normalized)) {
            return false;
        }
        return !supabaseAuthService.emailExistsInAuth(normalized);
    }

    public void assertEmailAvailable(String email) {
        if (!isEmailAvailable(email)) {
            throw new BusinessRuleException(EMAIL_TAKEN_MESSAGE);
        }
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
