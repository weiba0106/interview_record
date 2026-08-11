package com.interviewrecord.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.common.error.EmailAlreadyRegisteredException;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.auth.infrastructure.JpaEmailVerificationTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.defaults.infrastructure.JpaJobTypeRepository;
import com.interviewrecord.defaults.infrastructure.JpaPositionStatusRepository;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.support.FakeMailGateway;
import com.interviewrecord.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(FakeMailGateway.Config.class)
class RegistrationServiceTest extends MySqlIntegrationTestBase {

    @Autowired RegistrationService registrationService;
    @Autowired JpaUserRepository userRepository;
    @Autowired JpaJobTypeRepository jobTypeRepository;
    @Autowired JpaPositionStatusRepository statusRepository;
    @Autowired JpaUserPreferenceRepository preferenceRepository;
    @Autowired JpaEmailVerificationTokenRepository verificationTokenRepository;
    @Autowired FakeMailGateway fakeMailGateway;

    @BeforeEach
    void resetFakeMail() { fakeMailGateway.reset(); }

    @Test
    void registrationCreatesUnverifiedUserAndAllDefaults() {
        RegistrationResult result = registrationService.register(new RegisterCommand(
                " USER@example.com ", "Password123", "小林", "Asia/Shanghai", "127.0.0.1"));

        User user = userRepository.requireById(result.userId());
        assertThat(result.normalizedEmail()).isEqualTo("user@example.com");
        assertThat(user.isVerified()).isFalse();
        assertThat(jobTypeRepository.findNamesByUserId(result.userId())).containsExactly("秋招", "日常实习");
        assertThat(statusRepository.findNamesByUserIdOrderBySortOrder(result.userId()))
                .containsExactly("待投递", "已投递", "简历筛选中", "笔试/测评中", "面试中", "Offer", "未通过", "已放弃");
        assertThat(preferenceRepository.requireByUserId(result.userId()).theme()).isEqualTo(Theme.GRAPHITE_CORAL);
        assertThat(verificationTokenRepository.findByUserId(result.userId())).hasSize(1);
        assertThat(fakeMailGateway.verificationMessages()).hasSize(1);
    }

    @Test
    void registrationRejectsDuplicateEmailIgnoringCase() {
        registrationService.register(new RegisterCommand("user@example.com", "Password123", "小林", null, "127.0.0.1"));

        assertThatThrownBy(() -> registrationService.register(
                new RegisterCommand("USER@example.com", "Password123", "小王", null, "127.0.0.2")))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void registrationFallsBackToShanghaiForInvalidTimeZone() {
        RegistrationResult result = registrationService.register(new RegisterCommand(
                "timezone@example.com", "Password123", "小林", "not-a-time-zone", "127.0.0.1"));

        assertThat(preferenceRepository.requireByUserId(result.userId()).timeZone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void registrationRejectsPasswordsOutsideCreationPolicy() {
        assertThatThrownBy(() -> registrationService.register(new RegisterCommand(
                "short@example.com", "Pass123", "小林", null, "127.0.0.1"))).isInstanceOf(InvalidRegistrationException.class);
        assertThatThrownBy(() -> registrationService.register(new RegisterCommand(
                "letters@example.com", "PasswordOnly", "小林", null, "127.0.0.2"))).isInstanceOf(InvalidRegistrationException.class);
        assertThatThrownBy(() -> registrationService.register(new RegisterCommand(
                "digits@example.com", "12345678", "小林", null, "127.0.0.3"))).isInstanceOf(InvalidRegistrationException.class);
        assertThatThrownBy(() -> registrationService.register(new RegisterCommand(
                "bytes@example.com", "a".repeat(73) + "1", "小林", null, "127.0.0.4"))).isInstanceOf(InvalidRegistrationException.class);
    }

    @Test
    void mailFailureDoesNotRollbackCommittedAccountOrHashedToken() {
        fakeMailGateway.failVerificationDelivery();
        RegistrationResult result = registrationService.register(new RegisterCommand(
                "mail-failure@example.com", "Password123", "小林", null, "127.0.0.5"));

        assertThat(userRepository.requireById(result.userId()).email()).isEqualTo("mail-failure@example.com");
        assertThat(verificationTokenRepository.findByUserId(result.userId())).hasSize(1);
        assertThat(fakeMailGateway.verificationMessages()).isEmpty();
    }
}
