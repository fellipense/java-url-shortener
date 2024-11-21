package com.kingu.redirecturlshortener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // JSON Parser
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final S3Client s3Client = S3Client.builder().build();

    @Override // AWS Lambda default function
    public Map<String, Object> handleRequest(Map<String, Object> i, Context ctx) {
        
        // "https://dominio.com/UUID" -> "/UUID"
        String pathParams = (String) i.get("rawPath");

        // "/UUID" -> "UUID"
        String shortUrlCode = pathParams.replace("/", "");

        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required.");
        }

        // Creating request for object from S3
        GetObjectRequest getObjRequest = GetObjectRequest.builder()
            .bucket("kingu-url-shortener-storage")
            .key(shortUrlCode + ".json")
            .build();

        // The object will come through a data stream
        InputStream s3ObjStream;
        
        try {
            s3ObjStream = s3Client.getObject(getObjRequest);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching data from S3: " + e.getMessage(), e);
        }

        // Turn JSON String into Object
        UrlData urlData;
        try {
            urlData = objectMapper.readValue(s3ObjStream, UrlData.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing URL data: " + e.getMessage(), e);
        }
        
        // Current time milliseconds
        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        Map<String, Object> response = new HashMap<>();

        // If expiration time is exceeded
        if(currentTimeInSeconds < urlData.getExpirationTime()) {

            // 410 Gone: Resource permanently unavaliable 
            // https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Status/410
            response.put("statusCode", 410);
            response.put("body", "This URL has expired.");

            return response;
        }

        // 302 Found: Temporary moved to the location informed by header 'Location'
        // https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Status/302
        response.put("statusCode", 302);
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", urlData.getOriginalUrl());
        response.put("headers", headers);

        return response;
    }

}