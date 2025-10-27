package com.example.digitalWalletApp.mapper;

import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.dto.UserInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.name", target = "name")
    UserInfoResponse toDTO(User user, Double balance);
}

/*

Why we map only getName() & getEmail()
DTO only has name, email, balance.
So we ignore fields like password, id, role, age.

DTO has balance but User entity doesn’t.

@Override
public UserInfoResponse toDTO(User user, Double balance) {
    if (user == null) {
        return null;
    }

    UserInfoResponse dto = new UserInfoResponse(
        user.getName(),      // mapped from user.name
        user.getEmail(),     // mapped from user.email
        balance              // passed directly
    );
    return dto;
}

 */