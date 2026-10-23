package com.portfolio.rag.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrivacyAndVectorServiceTest {

    @Test
    void privacyGuard_ShouldRedactEmailsAndPhonesAndEncryptRoundTrip() {
        PrivacyGuardService guard = new PrivacyGuardService("MySuperSecretAESKey123456789012");

        String raw = "Contact me at john.doe@gmail.com or +48 501-202-303 regarding the secret note.";
        PrivacyGuardService.RedactionResult result = guard.redactSensitiveData(raw);

        assertThat(result.redactionCount()).isEqualTo(2);
        assertThat(result.sanitizedText()).contains("[REDACTED_EMAIL]");
        assertThat(result.sanitizedText()).contains("[REDACTED_PHONE]");
        assertThat(result.sanitizedText()).doesNotContain("john.doe@gmail.com");

        String encrypted = guard.encrypt(result.sanitizedText());
        assertThat(encrypted).isNotEqualTo(result.sanitizedText());
        assertThat(guard.decrypt(encrypted)).isEqualTo(result.sanitizedText());
    }
}
