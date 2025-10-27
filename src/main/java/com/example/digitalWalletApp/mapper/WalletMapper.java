package com.example.digitalWalletApp.mapper;

import com.example.digitalWalletApp.dto.LoadMoneyResponse;
import com.example.digitalWalletApp.dto.TransferResponse;
import com.example.digitalWalletApp.model.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "balance", source = "wallet.balance")
    @Mapping(target = "dailySpent", source = "wallet.dailySpent")
    @Mapping(target = "frozen", source = "wallet.frozen")
    LoadMoneyResponse toLoadMoneyResponse(Wallet wallet);

    @Mapping(target = "senderBalance", source = "wallet.balance")
    @Mapping(target = "frozen", source = "wallet.frozen")
    TransferResponse toTransferResponse(Wallet wallet);
}

/*

What MapStruct generates internally:

@Override
public LoadMoneyResponse toLoadMoneyResponse(Wallet wallet) {
    if (wallet == null) return null;

    LoadMoneyResponse dto = new LoadMoneyResponse();
    dto.setBalance(wallet.getBalance());
    dto.setDailySpent(wallet.getDailySpent());
    dto.setFrozen(wallet.getFrozen());
    return dto;
}

 */