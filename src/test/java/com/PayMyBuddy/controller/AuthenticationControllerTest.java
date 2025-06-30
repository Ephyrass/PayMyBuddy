package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private Model model;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;
    private UserAccount testUser;
    private Map<String, String> loginRequest;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");

        loginRequest = new HashMap<>();
        loginRequest.put("email", "test@example.com");
        loginRequest.put("password", "password");

        // Mock SecurityContext et Authentication
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // Tests pour les pages web

    @Test
    void index_shouldRedirectToDashboard_whenUserAuthenticated() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");

        // Act
        String result = authenticationController.index();

        // Assert
        assertEquals("redirect:/dashboard", result);
    }

    @Test
    void index_shouldReturnIndexPage_whenUserNotAuthenticated() {
        // Arrange
        when(authentication.getName()).thenReturn("anonymousUser");

        // Act
        String result = authenticationController.index();

        // Assert
        assertEquals("index", result);
    }

    @Test
    void loginPage_shouldReturnLoginView() {
        // Act
        String result = authenticationController.loginPage();

        // Assert
        assertEquals("login", result);
    }

    @Test
    void registerPage_shouldReturnRegisterView() {
        // Act
        String result = authenticationController.registerPage(model);

        // Assert
        assertEquals("register", result);
        verify(model).addAttribute(eq("user"), any(UserAccount.class));
    }

    @Test
    void registerUser_shouldRedirectToDashboard_whenRegistrationSuccessful() {
        // Arrange
        when(userAccountService.save(any(UserAccount.class))).thenReturn(testUser);

        // Act
        String result = authenticationController.registerUser(testUser);

        // Assert
        assertEquals("redirect:/dashboard", result);
        verify(userAccountService).save(testUser);
    }

    // Tests pour l'API

    @Test
    void login_shouldReturnUserData_whenCredentialsValid() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", "password");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = authenticationController.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals(1L, responseBody.get("id"));
        assertEquals("test@example.com", responseBody.get("email"));
        assertEquals("Test", responseBody.get("firstName"));
        assertEquals("User", responseBody.get("lastName"));
        assertEquals("Authentification réussie", responseBody.get("message"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsInvalid() {
        // Arrange
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        ResponseEntity<?> response = authenticationController.login(loginRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Email ou mot de passe incorrect", response.getBody());
    }

    @Test
    void logout_shouldReturnSuccessMessage() {
        // Act
        ResponseEntity<?> response = authenticationController.logout();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("Déconnexion réussie", responseBody.get("message"));
    }
}
