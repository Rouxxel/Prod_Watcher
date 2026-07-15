package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.prodwatch.api.dto.settings.SettingsUpdateRequest;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.repository.AuditEntryRepository;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.repository.WorkspaceSettingsRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.support.AbstractIntegrationTest;
import com.prodwatch.api.support.TestFixtures;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettingsServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private WorkspaceSettingsRepository workspaceSettingsRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EcosystemRepository ecosystemRepository;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private EntityManager entityManager;

    private CurrentUser admin;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.ADMIN_ID, AppRole.admin);
        admin = TestFixtures.currentUser(TestFixtures.ADMIN_ID, AppRole.admin);
    }

    @Test
    void getReturnsSeededSettings() {
        var response = settingsService.get(admin);

        assertThat(response.businessName()).isEqualTo("ProdWatch Demo Co.");
        assertThat(response.taxRate()).isEqualByComparingTo("0.16");
        assertThat(response.taxLabel()).isEqualTo("VAT");
        assertThat(response.businessMode()).isEqualTo("auto");
    }

    @Test
    void updateChangesTaxRateAndLogsAudit() {
        long before = auditEntryRepository.count();

        var response = settingsService.update(new SettingsUpdateRequest(null, null, new BigDecimal("0.10"), null, null, null, null), admin);

        assertThat(response.taxRate()).isEqualByComparingTo("0.10");
        assertThat(auditEntryRepository.count()).isGreaterThan(before);
        assertThat(auditEntryRepository.findAll().stream()
                        .anyMatch(e -> "SETTINGS_UPDATED".equals(e.getAction())))
                .isTrue();
    }

    @Test
    void bootstrapCreatesRowWhenMissing() {
        workspaceSettingsRepository.deleteById(TestFixtures.WORKSPACE_SETTINGS_ID);
        entityManager.flush();
        entityManager.clear();
        assertThat(workspaceSettingsRepository.findSingleton()).isEmpty();

        var response = settingsService.get(admin);

        assertThat(response.taxRate()).isEqualByComparingTo("0.16");
        assertThat(response.businessName()).isEmpty();
        assertThat(response.contactEmail()).isEmpty();
        assertThat(response.businessMode()).isEqualTo("auto");
        assertThat(response.taxLabel()).isEqualTo("Tax");
        assertThat(workspaceSettingsRepository.findSingleton()).isPresent();
    }
}
