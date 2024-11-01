package com.powerpuff.billServer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.powerpuff.billServer.model.Transaction;

@Service
public class OpenAIService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String openAIUrl;

    public String generateTransaction(Transaction transaction) {
        // 设置请求头信息，包括Authorization和Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 构建请求体，这里假设transaction的相关数据需要放入OpenAI请求的prompt中
        String prompt = buildPromptFromTransaction(transaction);
        String requestBody = String.format("{\"model\": \"text-davinci-003\", \"prompt\": \"%s\", \"max_tokens\": 50}", prompt);

        // 将headers和requestBody传入HttpEntity
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // 向OpenAI API发送请求
        try {
            String response = restTemplate.postForObject(openAIUrl, request, String.class);
            return response != null ? response : "No response from OpenAI";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    private String buildPromptFromTransaction(Transaction transaction) {
        return "Generate details based on the following transaction: " + transaction.toString();
    }
}
