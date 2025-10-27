package com.example.digitalWalletApp.mapper;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.dto.TransactionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "user.email", target = "userEmail")
    TransactionDTO toDTO(Transaction transaction); // Converts one entity object into one DTO object.
}

/*

How it works internally
MapStruct generates code at compile time.
So you never see it, but under the hood it does this:

@Override
public TransactionDTO toDTO(Transaction transaction) {
    if ( transaction == null ) {
        return null;
    }

    TransactionDTO dto = new TransactionDTO();
    dto.setId( transaction.getId() );
    dto.setAmount( transaction.getAmount() );
    dto.setType( transaction.getType() );
    dto.setTimestamp( transaction.getTimestamp() );

    if ( transaction.getUser() != null ) {
        dto.setUserEmail( transaction.getUser().getEmail() );
    }

    return dto;
}

 */