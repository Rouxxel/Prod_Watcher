package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.support.AbstractIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SignupEmailServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SignupEmailService signupEmailService;

    @Autowired
    private ProfileRepository profileRepository;

    @MockBean
    private SupabaseAuthService supabaseAuthService;

    @BeforeEach
    void cleanProfiles() {
        profileRepository.deleteAll();
        when(supabaseAuthService.emailExistsInAuth(anyString())).thenReturn(false);
    }

    @Test
    void emailAvailableWhenNotRegistered() {
        assertThat(signupEmailService.isEmailAvailable("new@test.local")).isTrue();
    }

    @Test
    void emailUnavailableWhenProfileExists() {
        profileRepository.save(Profile.create(UUID.randomUUID(), "taken@test.local", "Taken", true));

        assertThat(signupEmailService.isEmailAvailable("taken@test.local")).isFalse();
        assertThatThrownBy(() -> signupEmailService.assertEmailAvailable("taken@test.local"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(SignupEmailService.EMAIL_TAKEN_MESSAGE);
    }

    @Test
    void emailUnavailableWhenAuthUserExists() {
        when(supabaseAuthService.emailExistsInAuth("auth@test.local")).thenReturn(true);

        assertThat(signupEmailService.isEmailAvailable("auth@test.local")).isFalse();
    }

    @Test
    void emailCheckIsCaseInsensitive() {
        profileRepository.save(Profile.create(UUID.randomUUID(), "Owner@Test.Local", "Owner", true));

        assertThat(signupEmailService.isEmailAvailable("owner@test.local")).isFalse();
    }
}
