package com.powerpuff.billServer.controller;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.service.TransactionService;
import com.powerpuff.billServer.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public String addWithAI(@RequestBody Transaction transaction) {
        // 调用 OpenAIService 生成补充内容
        String aiResponse = openAIService.generateTransaction(transaction);

        // 这里可以根据 AI 返回的信息进行进一步的处理，例如解析、补充 Transaction 的内容等
//        transaction.setAiGeneratedDetails(aiResponse);  // 假设 Transaction 有一个字段来存储 AI 生成的内容
        transactionService.saveTransaction(transaction);

        return "New transaction is added with AI assistance. AI Response: " + aiResponse;
    }

//    @PostMapping("/addWithAI")
//    public String addWithAI(@RequestBody Transaction partialTransaction) {
//        // 使用AI生成完整的Transaction数据
//        String aiResponse = openAIService.generateTransaction(partialTransaction);
//
//        // 假设AI返回的数据可以直接解析成Transaction对象
//        Transaction aiGeneratedTransaction = parseAIResponseToTransaction(aiResponse, partialTransaction);
//
//        transactionService.saveTransaction(aiGeneratedTransaction);
//        return "New AI-generated transaction is added.";
//    }
//
//    private Transaction parseAIResponseToTransaction(String aiResponse, Transaction baseTransaction) {
//        // 根据实际的AI响应结构来解析，这里假设是JSON结构
//        // 结合用户提供的部分信息生成最终的Transaction对象
//        Transaction transaction = new Transaction();
//        // 设置必要的Transaction字段
//        return transaction;
//    }

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
