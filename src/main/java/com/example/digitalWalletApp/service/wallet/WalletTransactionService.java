package com.example.digitalWalletApp.service.wallet;

import com.example.digitalWalletApp.model.Transaction;
import com.example.digitalWalletApp.model.User;
import com.example.digitalWalletApp.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class WalletTransactionService {

    private final TransactionRepository transactionRepository;

    public WalletTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public boolean isDuplicate(String txnId) {
        return transactionRepository.findByTransactionId(txnId).isPresent();
    }

    public void recordLoadTransaction(User user, double amount, String txnId) {
        Transaction txn = new Transaction(user, amount, "SELF_CREDITED");
        txn.setTransactionId(txnId);
        transactionRepository.save(txn);
    }

    public void recordTransferTransactions(User sender, User receiver, double amount, String txnId) {
        Transaction debit = new Transaction(sender, amount, "DEBIT");
        debit.setTransactionId(txnId);
        transactionRepository.save(debit);

        Transaction credit = new Transaction(receiver, amount, "CREDIT");
        credit.setTransactionId(txnId + "-CREDIT");
        transactionRepository.save(credit);
    }
}
