package com.powerpuff.billServer.controller;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.service.TransactionService;
import com.powerpuff.billServer.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transaction")
@CrossOrigin
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private OpenAIService openAIService;

    // 手动添加账单
    @PostMapping("/add")
    public String add(@RequestBody Transaction transaction){
        transactionService.saveTransaction(transaction);
        return "New transaction is added";
    }

    // AI 帮助生成账单
    @PostMapping("/addWithAI")
    public ResponseEntity<?> addWithAI(@RequestBody Map<String, String> request) {
        try {
            String description = request.get("description");
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Description cannot be empty");
            }

            // 使用AI服务生成Transaction对象
            Transaction transaction = openAIService.generateTransactionFromDescription(description);
            
            // 保存到数据库
            transactionService.saveTransaction(transaction);

            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing transaction: " + e.getMessage());
        }
    }

    @GetMapping("/getAll")
    public List<Transaction> getAllTransaction(){
        return transactionService.getAllTransaction();
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id){
        transactionService.deleteTransaction(id);
        return "Transaction with ID " + id + " is logically delete (using_type set to 3)";

    }

}
