package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.LoginResponse;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.security.JwtService;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.role.entity.Role;
import com.ecommerce.role.repository.RoleRepository;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserStatus;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {

        // Configure the @Value field because Spring is not starting
        // during a Mockito unit test.
        ReflectionTestUtils.setField(
                authService,
                "jwtExpiration",
                3600000L
        );

        userRole = new Role();
        userRole.setId(1);
        userRole.setName("USER");

        user = new User();
        user.setId(10);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setRole(userRole);
        user.setStatus(UserStatus.ACTIVE);

        userResponse = new UserResponse();
        userResponse.setId(10);
        userResponse.setName("Test User");
        userResponse.setEmail("test@example.com");
        userResponse.setRole("USER");
        userResponse.setStatus(UserStatus.ACTIVE);
    }

    // REGISTRATION TESTS
    @Test
    void register_shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userMapper.toResponse(any(User.class)))
                .thenReturn(userResponse);

        UserResponse result = authService.register(request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("USER", result.getRole());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Test User", savedUser.getName());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals("USER", savedUser.getRole().getName());

        verify(passwordEncoder).encode("password123");
        verify(userMapper).toResponse(savedUser);
    }

    @Test
    void register_shouldNormalizeNameAndEmail() {

        RegisterRequest request = new RegisterRequest();
        request.setName("  Test User  ");
        request.setEmail("  TEST@EXAMPLE.COM  ");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userMapper.toResponse(any(User.class)))
                .thenReturn(userResponse);

        authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Test User", savedUser.getName());
        assertEquals("test@example.com", savedUser.getEmail());

        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    void register_shouldThrowConflictWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(userMapper);
    }

    @Test
    void register_shouldThrowWhenUserRoleIsNotConfigured() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(userMapper);
    }

    // LOGIN TESTS
    @Test
    void login_shouldLoginSuccessfullyAndReturnJwt() {

        LoginRequest request = LoginRequest.builder()
                .email("  TEST@EXAMPLE.COM  ")
                .password("password123")
                .build();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername("test@example.com")
                        .password("encoded-password")
                        .authorities("ROLE_USER")
                        .build();

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("test-jwt-token");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        LoginResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("test-jwt-token", result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(3600L, result.getExpiresIn());
        assertEquals(userResponse, result.getUser());

        ArgumentCaptor<Authentication> authenticationCaptor =
                ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationManager)
                .authenticate(authenticationCaptor.capture());

        UsernamePasswordAuthenticationToken submittedToken =
                (UsernamePasswordAuthenticationToken)
                        authenticationCaptor.getValue();

        assertEquals(
                "test@example.com",
                submittedToken.getPrincipal()
        );

        assertEquals(
                "password123",
                submittedToken.getCredentials()
        );

        verify(jwtService).generateToken(userDetails);
        verify(userRepository).findByEmail("test@example.com");
        verify(userMapper).toResponse(user);
    }

    @Test
    void login_shouldThrowWhenCredentialsAreInvalid() {

        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrong-password")
                .build();

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generateToken(any(UserDetails.class));
        verify(userRepository, never()).findByEmail(anyString());
        verifyNoInteractions(userMapper);
    }

    @Test
    void login_shouldThrowWhenUserIsNotFoundAfterAuthentication() {

        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername("test@example.com")
                        .password("encoded-password")
                        .authorities("ROLE_USER")
                        .build();

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("test-jwt-token");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.login(request)
        );

        verify(jwtService).generateToken(userDetails);
        verify(userRepository).findByEmail("test@example.com");
        verifyNoInteractions(userMapper);
    }
}

