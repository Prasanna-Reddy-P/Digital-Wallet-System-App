package com.example.digitalWalletApp.mapper;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.dto.TransactionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "user.email", target = "userEmail")
    TransactionDTO toDTO(Transaction transaction);
}
