package com.kingu.urlshortener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    // JSON parser
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override // AWS Lambda default function
    public Map<String, String> handleRequest(Map<String, Object> i, Context cntxt) {
        
        // JSON String input
        String body = (String) i.get("body");
        
        // JSON -> Map
        Map<String, String> bodyMap;
        
        // Maybe the input isn't "parsable"
        try {
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (Exception exception) {
            throw new RuntimeException("Error parsing JSON body: " + exception.getMessage(), exception);
        }

        // Get input URL (the long one) and it's expiration time
        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");

        // Generate a random UUID String for the original URL
        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);

        // Output
        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);
    
        return response;
    }

}