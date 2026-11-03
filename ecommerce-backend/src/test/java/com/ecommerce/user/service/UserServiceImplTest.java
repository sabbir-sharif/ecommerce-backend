package com.ecommerce.user.service;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.role.entity.Role;
import com.ecommerce.role.repository.RoleRepository;
import com.ecommerce.user.dto.UserCreateRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserUpdateRequest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserStatus;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role role;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {

        role = new Role();
        role.setId(1);
        role.setName("USER");

        user = new User();
        user.setId(10);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        userResponse = new UserResponse();
        userResponse.setId(10);
        userResponse.setName("Test User");
        userResponse.setEmail("test@example.com");
        userResponse.setRole("USER");
        userResponse.setStatus(UserStatus.ACTIVE);
    }

    private UserCreateRequest createRequest() {

        UserCreateRequest request = new UserCreateRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("encoded-password");
        request.setRoleId(1);

        return request;
    }

    private UserUpdateRequest updateRequest(
            String name, String email) {

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName(name);
        request.setEmail(email);

        return request;
    }

    // 1. CREATE TESTS
    @Test
    void create_shouldCreateUserSuccessfully() {

        UserCreateRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(roleRepository.findById(1))
                .thenReturn(Optional.of(role));

        when(userMapper.toEntity(request))
                .thenReturn(user);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        UserResponse result = userService.create(request);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertSame(role, user.getRole());

        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void create_shouldThrowWhenEmailAlreadyExists() {

        UserCreateRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> userService.create(request)
        );

        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).findById(anyInt());
    }

    @Test
    void create_shouldThrowWhenRoleDoesNotExist() {

        UserCreateRequest request = createRequest();

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(roleRepository.findById(1))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.create(request)
        );

        verify(userRepository, never()).save(any());
    }

    // 2. GET CURRENT USER TESTS
    @Test
    void getCurrentUser_shouldReturnUserSuccessfully() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        UserResponse result =
                userService.getCurrentUser("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());

        verify(userRepository).findByEmail("test@example.com");
        verify(userMapper).toResponse(user);
    }

    @Test
    void getCurrentUser_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getCurrentUser("missing@example.com")
        );

        verify(userMapper, never()).toResponse(any());
    }

    // 3. GET USER BY ID TESTS
    @Test
    void getById_shouldReturnUserSuccessfully() {

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        UserResponse result = userService.getById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());

        verify(userRepository).findById(10);
    }

    @Test
    void getById_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getById(99)
        );

        verify(userMapper, never()).toResponse(any());
    }

    // 4. GET ALL USERS TESTS
    @Test
    void getAll_shouldReturnAllUsers() {

        User secondUser = new User();
        secondUser.setId(11);
        secondUser.setName("Second User");
        secondUser.setEmail("second@example.com");
        secondUser.setRole(role);
        secondUser.setStatus(UserStatus.ACTIVE);

        UserResponse secondResponse = new UserResponse();
        secondResponse.setId(11);
        secondResponse.setName("Second User");
        secondResponse.setEmail("second@example.com");

        when(userRepository.findAll())
                .thenReturn(List.of(user, secondUser));

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        when(userMapper.toResponse(secondUser))
                .thenReturn(secondResponse);

        List<UserResponse> result = userService.getAll();

        assertEquals(2, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        assertEquals("second@example.com", result.get(1).getEmail());

        verify(userRepository).findAll();
        verify(userMapper, times(2)).toResponse(any(User.class));
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNoUsersExist() {

        when(userRepository.findAll())
                .thenReturn(List.of());

        List<UserResponse> result = userService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userMapper, never()).toResponse(any());
    }

    // 5. UPDATE USER TESTS
    @Test
    void update_shouldUpdateUserSuccessfully() {

        UserUpdateRequest request =
                updateRequest("Updated User", "updated@example.com");

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail("updated@example.com"))
                .thenReturn(false);

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        UserResponse result = userService.update(10, request);

        assertNotNull(result);
        assertEquals("Updated User", user.getName());
        assertEquals("updated@example.com", user.getEmail());

        verify(userRepository).save(user);
    }

    @Test
    void update_shouldThrowWhenNewEmailAlreadyExists() {

        UserUpdateRequest request =
                updateRequest("Updated User", "taken@example.com");

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail("taken@example.com"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> userService.update(10, request)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldNotCheckDuplicateWhenEmailIsUnchanged() {

        UserUpdateRequest request =
                updateRequest("Updated User", "test@example.com");

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        UserResponse result = userService.update(10, request);

        assertNotNull(result);
        assertEquals("Updated User", user.getName());
        assertEquals("test@example.com", user.getEmail());

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldThrowWhenUserDoesNotExist() {

        UserUpdateRequest request =
                updateRequest("Updated User", "updated@example.com");

        when(userRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.update(99, request)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateOnlyNameWhenEmailIsNull() {

        UserUpdateRequest request =
                updateRequest("Name Only Updated", null);

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(userResponse);

        UserResponse result = userService.update(10, request);

        assertNotNull(result);
        assertEquals("Name Only Updated", user.getName());
        assertEquals("test@example.com", user.getEmail());

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(user);
    }

    // 6. DELETE USER TESTS
    @Test
    void delete_shouldDeleteUserSuccessfully() {

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        userService.delete(10);

        verify(userRepository).findById(10);
        verify(userRepository).delete(user);
    }

    @Test
    void delete_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.delete(99)
        );

        verify(userRepository, never()).delete(any());
    }
}

