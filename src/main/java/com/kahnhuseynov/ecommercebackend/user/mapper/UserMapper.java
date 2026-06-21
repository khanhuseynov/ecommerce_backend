package com.kahnhuseynov.ecommercebackend.user.mapper;

import com.kahnhuseynov.ecommercebackend.user.dto.UserRegisterRequest;
import com.kahnhuseynov.ecommercebackend.user.dto.UserResponse;
import com.kahnhuseynov.ecommercebackend.user.entity.Role;
import com.kahnhuseynov.ecommercebackend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    // Converts incoming registration request DTO to User entity
    User toEntity(UserRegisterRequest request);

    // Converts User entity to response DTO (excluding password automatically)
    UserResponse toResponse(User user);

    // Custom default method to map Set<Role> to Set<String> for the response DTO
    // Without this, MapStruct will throw a compilation error
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}