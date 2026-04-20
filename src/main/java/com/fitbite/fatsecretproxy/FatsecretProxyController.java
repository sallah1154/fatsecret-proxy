package com.fitbite.fatsecretproxy;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class FatsecretProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    // 🔑 YOUR REAL KEYS (as requested)
    private final String CLIENT_ID = "d93a1c71a19841cfbbef59d49370fe3e";
    private final String CLIENT_SECRET = "685ad09e5b5942ba9c6764ad750825a7";

    // STEP 1 — Get OAuth token
    private String getAccessToken() {

        String tokenUrl = "https://oauth.fatsecret.com/connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", CLIENT_ID);
        body.add("client_secret", CLIENT_SECRET);
        body.add("scope", "basic barcode");

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response.getBody() == null ||
                response.getBody().get("access_token") == null) {
            throw new RuntimeException("Failed to retrieve access token.");
        }

        return (String) response.getBody().get("access_token");
    }

    // STEP 2 — Proxy request
    @GetMapping("/fatsecret")
    public ResponseEntity<String> proxy(@RequestParam("url") String encodedUrl) {

        try {
            // Decode URL from Android
            String url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);

            System.out.println("Decoded URL: " + url);

            // 🔒 SECURITY: only allow FatSecret API calls
            if (!url.startsWith("https://platform.fatsecret.com/")) {
                return ResponseEntity.status(400).body("Invalid URL");
            }

            // Get access token
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Send request to FatSecret
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            System.out.println("Response Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Proxy Error: " + e.getMessage());
        }
    }
}