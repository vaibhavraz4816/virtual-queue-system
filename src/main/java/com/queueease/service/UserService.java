package com.queueease.service;

import com.queueease.dto.RegisterRequest;
import com.queueease.entity.User;

public interface UserService {
    User registerOwner(RegisterRequest request);
    User findByEmail(String email);
    User getCurrentOwner();
}
