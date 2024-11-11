package com.powerpuff.billServer.controller;

import com.powerpuff.billServer.model.Transaction;
import com.powerpuff.billServer.service.TransactionService;
import com.powerpuff.billServer.service.OpenAIService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.json.JSONObject;

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
    @PostMapping("/save")
    public String add(@RequestBody Transaction transaction){
        transactionService.saveTransaction(transaction);
        return "New transaction is added";
    }

    // AI 帮助生成账单
    @PostMapping("/addWithAI")
    public ResponseEntity<String> addWithAI( @Parameter(description = "User's message for AI processing",
            schema = @Schema(defaultValue = "Bought groceries at supersotre for $72.50 on 08/15/2024"))
                                                 @RequestBody String userMessage) {
        try {
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Message cannot be empty");
            }

            // Call the AI service and get the response
            JSONObject openAIResponse = openAIService.callOpenAI(userMessage);

            // Use AI service to generate and save the Transaction object
            Transaction transaction = openAIService.parseAndSaveTransaction(openAIResponse);

            // Save the transaction to the database
            transactionService.saveTransaction(transaction);

            // Return the formatted JSON response with indentation
            return ResponseEntity.ok(openAIResponse.toString(4));  // 4 spaces for indentation
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing transaction: " + e.getMessage());
        }
    }

    @GetMapping("/getAll")
    public List<Transaction> getAllTransaction(
            @RequestParam(value = "sortOrder", required = false) String sortOrder){
        return transactionService.getAllTransaction(sortOrder);
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id){
        transactionService.deleteTransaction(id);
        return "Transaction with ID " + id + " is logically delete (using_type set to 3)";

    }

}
