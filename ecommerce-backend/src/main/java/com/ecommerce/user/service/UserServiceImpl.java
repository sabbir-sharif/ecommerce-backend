package com.ecommerce.user.service;

import com.ecommerce.user.dto.UserCreateRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserUpdateRequest;
import com.ecommerce.role.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserStatus;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.role.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse create(UserCreateRequest request) {

        // 1. Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. Resolve roleId → Role entity
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() ->
                        new RuntimeException("Role not found"));

        // 3. DTO → Entity
        User user = userMapper.toEntity(request);

        // 4. Set service-controlled fields
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        // 5. Save
        User savedUser = userRepository.save(user);

        // 6. Entity → Response DTO
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getById(int id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAll() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse update(
            int id,
            UserUpdateRequest request) {

        // 1. Find existing user
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Check email only if it changed
        if (request.getEmail() != null
                && !user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new RuntimeException("Email already exists");
        }

        // 3. Update allowed fields
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        // 4. Save
        User updatedUser = userRepository.save(user);

        // 5. Return DTO
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void delete(int id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        userRepository.delete(user);
    }
}
