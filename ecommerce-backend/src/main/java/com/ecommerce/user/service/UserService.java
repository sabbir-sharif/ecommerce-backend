package com.ecommerce.user.service;

import com.ecommerce.user.dto.UserCreateRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserUpdateRequest;

import java.util.List;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    UserResponse getById(int id);

    List<UserResponse> getAll();

    UserResponse update(int id, UserUpdateRequest request);

    void delete(int id);
}
