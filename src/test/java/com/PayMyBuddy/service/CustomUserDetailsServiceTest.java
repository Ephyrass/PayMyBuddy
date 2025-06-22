package com.PayMyBuddy.service;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void loadUserByUsername_withExistingEmail_shouldReturnUserDetails() {
        // Arrange
        when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isAccountNonLocked());

        boolean hasUserRole = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));
        assertTrue(hasUserRole);

        verify(userAccountRepository).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_withNonExistingEmail_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () ->
            customUserDetailsService.loadUserByUsername("nonexistent@example.com"));
        verify(userAccountRepository).findByEmail("nonexistent@example.com");
    }
}
