package com.powerpuff.billServer.service;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.model.UsingType;
import com.powerpuff.billServer.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService{
    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> getAllTransaction(){
        return transactionRepository.findByUsingTypeNot(UsingType.DELETED);
    }

    @Override
    public String deleteTransaction(Integer id){
        Transaction transaction = transactionRepository.findById(id).orElse(null);
        if(transaction != null){
            transaction.setUsingType(UsingType.DELETED);
            transactionRepository.save(transaction);
            return "Deleted successfully.";
        }else {
            return "can not find this record.";
        }
    }
}
