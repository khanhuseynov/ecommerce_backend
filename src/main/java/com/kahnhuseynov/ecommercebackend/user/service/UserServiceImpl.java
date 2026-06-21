package com.kahnhuseynov.ecommercebackend.user.service;

import com.kahnhuseynov.ecommercebackend.user.exception.BusinessException;
import com.kahnhuseynov.ecommercebackend.user.exception.ResourceNotFoundException;
import com.kahnhuseynov.ecommercebackend.user.dto.UserRegisterRequest;
import com.kahnhuseynov.ecommercebackend.user.dto.UserResponse;
import com.kahnhuseynov.ecommercebackend.user.entity.Role;
import com.kahnhuseynov.ecommercebackend.user.entity.User;
import com.kahnhuseynov.ecommercebackend.user.mapper.UserMapper;
import com.kahnhuseynov.ecommercebackend.user.repository.RoleRepository;
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already in use.");
        }

        User user = userMapper.toEntity(request);

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role 'ROLE_USER' not found.")
                );

        user.setEnabled(true);
        user.setRoles(Set.of(role));

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }
}