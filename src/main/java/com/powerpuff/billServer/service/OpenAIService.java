package com.powerpuff.billServer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerpuff.billServer.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import java.time.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate= new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    public Transaction generateTransactionFromDescription(String description) {
        try {
            String prompt = buildPrompt(description);
            String aiResponse = callOpenAI(prompt);
            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            throw new RuntimeException("Error generating transaction from AI: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String userContent) {
        return  "You are an AI assistant specialized in categorizing expenses for a personal finance app. "
                + "Your task is to analyze the given expense description and categorize it according to predefined categories. "
                + "You should also extract the amount, the currency, the date of the expense if available, and the location if provided.\n\n"
                + "The content output by the user is marked as <content></content>. Please analyze the input content and complete the task based on the following requirements:\n"
                + "1. Identify the most likely intention of the customer and record it as transaction_type;\n"
                + "2. Extract valuable information from <content> and summarize it in a short sentence, recording it as <description>;\n"
                + "3. According to the extracted intent, further extract specific information based on the following requirements, and organize the above <description> together into JSON format for return;\n"
                + "4. For the date (date) and time (time) fields in JSON, please combine the current time <current_time> to process it into the corresponding \"YYYY-MM-DD\" format and \"HH:MM\" format.\n"
                + "5. No need to return the test process; just return the JSON result obtained from the final analysis. Do not return any other content, including comments, and ensure that the final return format is in JSON format.\n\n"
                + "The logic of quality judgment and processing is as follows:\n"
                + "1. If the customer mentions a transaction, consumption, or order, it means they want to record an expenditure (outcome). Please extract specific information from the content, including date, location, amount, participants, purpose (description), and payment method (payment_method). Analyze which category the expenditure belongs to in combination with the expenditure classification list (<category_list>), recorded as category_en, such as dining consumption record FOOD_AND_DINING.\n\n"
                + "<category_list>\n"
                + "FOOD_AND_DINING,\n"
                + "SHOPPING,\n"
                + "TRANSPORTATION,\n"
                + "HOUSING,\n"
                + "ENTERTAINMENT_AND_RECREATION,\n"
                + "HEALTH_AND_INSURANCE,\n"
                + "EDUCATION_AND_TRAINING,\n"
                + "COMMUNICATION_AND_TECHNOLOGY,\n"
                + "GIFTS_AND_DONATIONS,\n"
                + "FAMILY_OBLIGATIONS,\n"
                + "DEBT_AND_SAVINGS,\n"
                + "TAXES,\n"
                + "OTHER_EXPENSE\n"
                + "</category_list>\n\n"
                + "Please return in JSON format. For example, when the content is \"Bought groceries at Walmart for $72.50 on 08/15/2024\", the final output should be in the following JSON format:\n"
                + "{\n"
                + "  \"transaction_type\": \"outcome\",\n"
                + "  \"date\": \"2024-08-15\",\n"
                + "  \"time\": \"18:30\",\n"
                + "  \"amount\": 72.50,\n"
                + "  \"currency\": \"USD\",\n"
                + "  \"category\": \"FOOD_AND_DINING\",\n"
                + "  \"description\": \"Bought groceries at Walmart\",\n"
                + "  \"participants\": null,\n"
                + "  \"location\": \"Walmart\",\n"
                + "  \"payment_method\": null\n"
                + "}\n\n"
                + "2. If the customer tells me an income or funds coming in, it means that he wants to record an income (income). Please extract specific information from the content, including date (date), amount (amount), reason for income (description), income type (income_type), and payment method (payment_method). Please analyze which category the expenditure belongs to in conjunction with the income category list (<income_category_list>), and record it as category_en.\n\n"
                + "<income_category_list>\n"
                + "Salary\n"
                + "Bonuses and Allowances\n"
                + "Investment Income\n"
                + "Side Income\n"
                + "Gifts and Inheritance\n"
                + "Other Income\n"
                + "</income_category_list>\n\n"
                + "Please return in JSON format. For example, when the content is \"Got Bonuses and Allowances for $320 on 08/15/2024\", the final output should be in the following JSON format:\n"
                + "{\n"
                + "  \"transaction_type\": \"income\",\n"
                + "  \"date\": \"2024-08-15\",\n"
                + "  \"time\": \"18:30\",\n"
                + "  \"amount\": 320,\n"
                + "  \"currency\": \"USD\",\n"
                + "  \"category\": \"Bonuses and Allowances\",\n"
                + "  \"description\": \"Got Bonuses and Allowances\",\n"
                + "  \"participants\": null,\n"
                + "  \"location\": null,\n"
                + "  \"payment_method\": null\n"
                + "}\"\n"
                + "<content>" + userContent + "</content>\n"
                + "<current_time>" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</current_time>";
    }

    public String callOpenAI(String prompt) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                return executeOpenAICall(prompt);
            } catch (HttpServerErrorException e) {
                lastException = e;
                log.warn("OpenAI API server error (attempt {}/{}): {}", 
                    retryCount + 1, MAX_RETRIES, e.getMessage());
                
                if (retryCount < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (retryCount + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry was interrupted", ie);
                    }
                }
            } catch (Exception e) {
                // For non-server errors, throw immediately
                throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
            }
            retryCount++;
        }
        
        // If all retries failed
        throw new RuntimeException("Failed to call OpenAI API after " + MAX_RETRIES + " attempts: " 
            + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private String executeOpenAICall(String prompt) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(10);
        
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();
        
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(15));
        
        RestTemplate restTemplate = new RestTemplate(factory);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant that analyzes financial transactions."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("OpenAI API returned error status: " + response.getStatusCode());
            }

            String responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from OpenAI");
            }

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response");
            }

            Map<String, Object> choice = choices.get(0);
            String content = null;
            if (choice.get("message") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                content = message != null ? (String) message.get("content") : null;
            }

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Empty content in OpenAI response");
            }

            return content;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("OpenAI API error: {}", e.getResponseBodyAsString(), e);
            throw e;
        }
    }

    private Transaction parseAIResponse(String aiResponse) throws Exception {
        if (aiResponse == null || aiResponse.isEmpty()) {
            throw new RuntimeException("AI response is empty");
        }

        // Check if `aiResponse` is wrapped in quotes (indicating a JSON string, not an object)
        if (aiResponse.startsWith("\"") && aiResponse.endsWith("\"")) {
            aiResponse = aiResponse.substring(1, aiResponse.length() - 1); // Remove surrounding quotes
            aiResponse = aiResponse.replace("\\\"", "\""); // Unescape any quotes within the JSON
        }

        try {
            // Attempt to parse the JSON
            Map<String, Object> responseMap = objectMapper.readValue(aiResponse, Map.class);

            Transaction transaction = new Transaction();
            transaction.setTransactionType((String) responseMap.get("transaction_type"));

            // Process date and time
            String dateString = (String) responseMap.get("date");
            String timeString = (String) responseMap.get("time");
            if (dateString != null && timeString != null) {
                String dateTimeString = dateString + " " + timeString + ":00";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, formatter);
                transaction.setCreatedAt(dateTime);
                transaction.setUpdatedAt(dateTime);
                transaction.setTransactionAt(dateTime);
            }

            // Handle amount
            Object amountObj = responseMap.get("amount");
            BigDecimal amount = amountObj instanceof Number ?
                    new BigDecimal(amountObj.toString()) : BigDecimal.ZERO;
            transaction.setTotalAmount(amount);

            transaction.setCurrency((String) responseMap.get("currency"));
            transaction.setCategory((String) responseMap.get("category"));
            transaction.setDescription((String) responseMap.get("description"));
            transaction.setLocation((String) responseMap.get("location"));
            transaction.setPaymentMethod((String) responseMap.get("payment_method"));

            return transaction;
        } catch (JsonProcessingException e) {
            // Log and throw a custom error if JSON parsing fails
            throw new IllegalArgumentException("Failed to parse AI response as JSON: " + aiResponse, e);
        }
    }
}
