package org.altcha.example.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.altcha.altcha.Altcha;
import org.altcha.altcha.Altcha.Algorithm;
import org.altcha.altcha.Altcha.Challenge;
import org.altcha.altcha.Altcha.ChallengeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/")
public class WebController {

    @Value("${ALTCHA_HMAC_KEY:secret-key}") // Default value if env variable is not set
    private String hmacKey;

    @GetMapping
    protected void home(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");

        response.getWriter().println("ALTCHA server demo endpoints:\n\n" +
        "GET /altcha - use this endpoint as challengeurl for the widget\n" +
        "POST /submit - use this endpoint as the form action\n" +
        "POST /submit_spam_filter - use this endpoint for form submissions with spam filtering");
    }

    @GetMapping("/altcha")
    @CrossOrigin(origins = "*") 
    public Challenge altcha() {
        try {
            ChallengeOptions options = new ChallengeOptions();
            options.algorithm = Algorithm.SHA256;
            options.hmacKey = hmacKey;

            return Altcha.createChallenge(options);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating challenge", e);
        }
    }

    @PostMapping("/submit")
    @CrossOrigin(origins = "*") 
    public Map<String, Object> submit(@RequestParam Map<String, String> formData) {
        Map<String, Object> response = new HashMap<>();
        try {
            String payload = formData.get("altcha");

            if (payload == null) {
                response.put("success", false);
                response.put("error", "'altcha' field is missing");
                return response;
            }

            boolean isValid = Altcha.verifySolution(payload, hmacKey, true);
            response.put("success", isValid);
            response.put("data", formData);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error verifying solution: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/submit_spam_filter")
    @CrossOrigin(origins = "*")
    public Map<String, Object> submitSpamFilter(@RequestParam Map<String, String> formData) {
        Map<String, Object> response = new HashMap<>();
        try {
            String payload = formData.get("altcha");

            Altcha.ServerSignatureVerification verification;

            verification = Altcha.verifyServerSignature(payload, hmacKey);

            response.put("success", verification.verified);
            response.put("data", formData);
            response.put("verificationData", verification.verificationData);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Error verifying server signature: " + e.getMessage());
        }
        return response;
    }
}
