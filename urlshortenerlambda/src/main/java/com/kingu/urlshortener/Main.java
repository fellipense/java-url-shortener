package com.kingu.urlshortener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    // JSON parser
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final S3Client s3Client = S3Client.builder().build();

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
        long expirationTimeInSeconds = Long.parseLong(expirationTime) * 3600;

        // Generate a random UUID String for the original URL
        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);

        // Turning into a object, to later create a JSON String
        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        // Stringify the object and save it on S3 bucket 
        try {

            String urlDataJson = objectMapper.writeValueAsString(urlData);

            PutObjectRequest request = PutObjectRequest.builder()
                .bucket("kingu-url-shotener-storage")
                .key(shortUrlCode + ".json")
                .build();
            
            s3Client.putObject(request, RequestBody.fromString(urlDataJson));
            
        } catch (Exception e) {

            throw new RuntimeException("Error saving URL data to S3: " + e.getMessage(), e);

        }

        // Output
        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);
    
        return response;
    }

}