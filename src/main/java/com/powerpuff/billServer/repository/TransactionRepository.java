package com.powerpuff.billServer.repository;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.model.UsingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Integer> {

    // list transactions by creat time
    List<Transaction> findByUsingTypeNotOrderByCreatedAtDesc(UsingType usingType);
    List<Transaction> findByUsingTypeNotOrderByCreatedAtAsc(UsingType usingType);


    // find transactions by category, usingType is not equal to DELETED (value 3), sorting by create time //desc or asc
    List<Transaction> findByUsingTypeNotAndCategoryOrderByCreatedAtDesc(UsingType usingType, String category);

    List<Transaction> findByUsingTypeNotAndCategoryOrderByCreatedAtAsc(UsingType usingType, String category);
}
