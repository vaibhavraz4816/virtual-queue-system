package com.queueease.service.impl;

import com.queueease.dto.RegisterRequest;
import com.queueease.entity.Shop;
import com.queueease.entity.User;
import com.queueease.entity.enums.Role;
import com.queueease.entity.enums.ShopStatus;
import com.queueease.exception.ResourceNotFoundException;
import com.queueease.repository.ShopRepository;
import com.queueease.repository.UserRepository;
import com.queueease.security.SecurityUtils;
import com.queueease.service.UserService;
import com.queueease.util.SlugUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           ShopRepository shopRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerOwner(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        User user = new User(
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_OWNER
        );
        user = userRepository.save(user);

        // Auto-create shop for owner
        String baseSlug = SlugUtils.toSlug(request.getShopName());
        String finalSlug = baseSlug;
        int counter = 1;
        while (shopRepository.existsBySlug(finalSlug)) {
            finalSlug = baseSlug + "-" + counter++;
        }

        Shop shop = new Shop(
                user,
                request.getShopName().trim(),
                finalSlug,
                request.getShopAddress(),
                request.getShopPhone(),
                request.getShopDescription(),
                request.getAverageServiceMinutes()
        );
        shop.setStatus(ShopStatus.OPEN);
        if (request.getCategory() != null) {
            shop.setCategory(request.getCategory());
        }
        shopRepository.save(shop);

        return user;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email != null ? email.trim().toLowerCase() : null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public User getCurrentOwner() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated user found in session"));
        return findByEmail(email);
    }
}
