package com.powerpuff.billServer.repository;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.model.UsingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Integer> {
    // 查找 using_type 不等于 3 的交易
    List<Transaction> findByUsingTypeNot(UsingType usingType);
}
