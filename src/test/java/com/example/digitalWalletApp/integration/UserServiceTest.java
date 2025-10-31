package com.example.digitalWalletApp.integration;

import com.example.digitalWalletApp.config.JwtUtil;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.repository.UserRepository;
import com.example.digitalWalletApp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private String validToken = "Bearer valid.jwt.token";
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User("John", "john@example.com", "password123");
        logger.info("\n\n✅ Test setup complete — Mock user created: {}", mockUser.getEmail());
    }

    // ------------------------------------------------------------
    // ✅ POSITIVE TEST — Valid Token & User Exists
    // ------------------------------------------------------------
    @Test
    void getUserFromToken_ShouldReturnUser_WhenTokenIsValid() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getUserFromToken_ShouldReturnUser_WhenTokenIsValid");
        logger.info("------------------------------");

        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid.jwt.token")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        logger.info("🧩 Mock setup complete — Valid token & existing user = {}", mockUser.getEmail());

        User result = userService.getUserFromToken(validToken);

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository).findByEmail("john@example.com");
        logger.info("✅ Test passed — User successfully returned from token → {}", result.getEmail());
        logger.info("------------------------------\n\n");
    }

    // ------------------------------------------------------------
    // ❌ NEGATIVE TEST — Invalid Header Format
    // ------------------------------------------------------------
    @Test
    void getUserFromToken_ShouldReturnNull_WhenHeaderIsInvalid() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getUserFromToken_ShouldReturnNull_WhenHeaderIsInvalid");
        logger.info("------------------------------");

        User result1 = userService.getUserFromToken(null);
        User result2 = userService.getUserFromToken("InvalidToken");

        assertNull(result1);
        assertNull(result2);
        verifyNoInteractions(jwtUtil, userRepository);
        logger.info("✅ Test passed — Invalid header correctly returned null user");
        logger.info("------------------------------\n\n");
    }

    // ------------------------------------------------------------
    // ❌ NEGATIVE TEST — Invalid Token
    // ------------------------------------------------------------
    @Test
    void getUserFromToken_ShouldReturnNull_WhenTokenInvalid() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getUserFromToken_ShouldReturnNull_WhenTokenInvalid");
        logger.info("------------------------------");

        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(false);
        logger.info("🧩 Mock setup complete — Token validation set to false");

        User result = userService.getUserFromToken(validToken);

        assertNull(result);
        verify(jwtUtil).validateToken("valid.jwt.token");
        logger.info("✅ Test passed — Invalid token correctly returned null user");
        logger.info("------------------------------\n\n");
    }

    // ------------------------------------------------------------
    // ❌ NEGATIVE TEST — User Not Found in Database
    // ------------------------------------------------------------
    @Test
    void getUserFromToken_ShouldReturnNull_WhenUserNotFound() {
        logger.info("\n\n------------------------------");
        logger.info("🔹 TEST START: getUserFromToken_ShouldReturnNull_WhenUserNotFound");
        logger.info("------------------------------");

        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid.jwt.token")).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        logger.info("🧩 Mock setup complete — Token valid, but user not found in DB");

        User result = userService.getUserFromToken(validToken);

        assertNull(result);
        logger.info("✅ Test passed — Non-existent user correctly returned null");
        logger.info("------------------------------\n\n");
    }
}
