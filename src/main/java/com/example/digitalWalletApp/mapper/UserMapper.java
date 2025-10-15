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
