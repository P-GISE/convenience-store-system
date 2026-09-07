package com.example.ordersystem2.service;

import com.example.ordersystem2.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlServiceTest {

    private final AccessControlService accessControlService = new AccessControlService();

    @Test
    void anonymousSessionIsNotLoggedIn() {
        MockHttpSession session = new MockHttpSession();

        assertThat(accessControlService.isLoggedIn(session)).isFalse();
        assertThat(accessControlService.redirectFor(session)).isEqualTo("redirect:/login");
    }

    @Test
    void headquartersRoleIsDetectedFromSessionString() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", "HEADQUARTERS");

        assertThat(accessControlService.isLoggedIn(session)).isTrue();
        assertThat(accessControlService.isHeadquarters(session)).isTrue();
        assertThat(accessControlService.hasRole(session, UserRole.STORE_OWNER)).isFalse();
    }

    @Test
    void missingRoleDefaultsToStoreOwnerForOldSessions() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        assertThat(accessControlService.isStoreOwner(session)).isTrue();
    }
}
