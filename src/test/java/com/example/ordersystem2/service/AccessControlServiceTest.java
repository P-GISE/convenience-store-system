package com.example.ordersystem2.service;

import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.repository.UserRepository;
import java.util.Optional;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AccessControlService accessControlService = new AccessControlService(users);

    @Test
    void anonymousSessionIsNotLoggedIn() {
        MockHttpSession session = new MockHttpSession();

        assertThat(accessControlService.isLoggedIn(session)).isFalse();
        assertThat(accessControlService.redirectFor(session)).isEqualTo("redirect:/login");
    }

    @Test
    void headquartersRoleComesFromDatabase() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", "HEADQUARTERS");
        User user = new User("synthetic", "encoded");
        user.setRole(UserRole.HEADQUARTERS);
        when(users.findById(1L)).thenReturn(Optional.of(user));

        assertThat(accessControlService.isLoggedIn(session)).isTrue();
        assertThat(accessControlService.isHeadquarters(session)).isTrue();
        assertThat(accessControlService.hasRole(session, UserRole.STORE_OWNER)).isFalse();
    }

    @Test
    void stalePrivilegedSessionDoesNotOverrideCustomerDatabaseRole() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", "HEADQUARTERS");
        when(users.findById(1L)).thenReturn(Optional.of(new User("synthetic", "encoded")));

        assertThat(accessControlService.isStoreOwner(session)).isFalse();
        assertThat(accessControlService.isHeadquarters(session)).isFalse();
        assertThat(accessControlService.redirectFor(session)).isEqualTo("redirect:/shop");
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThat(accessControlService.isLoggedIn(session)).isFalse();
    }
}
