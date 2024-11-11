package com.powerpuff.billServer.repository;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.model.UsingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Integer> {
    // find transactions where usingType is not equal to DELETED (value 3)
//    List<Transaction> findByUsingTypeNot(UsingType usingType);

    // list transactions by creat time
    List<Transaction> findByUsingTypeNotOrderByCreatedAtDesc(UsingType usingType);
    List<Transaction> findByUsingTypeNotOrderByCreatedAtAsc(UsingType usingType);
}
