package vn.affkit.auth.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        log.info("=== RESEND_API_KEY prefix: {} ===", apiKey.substring(0, Math.min(12, apiKey.length())));
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        send(toEmail, "Xac nhan email AffKit", buildVerifyEmailHtml(toEmail, verifyLink));
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        send(toEmail, "Dat lai mat khau AffKit", buildResetPasswordHtml(toEmail, resetLink));
    }

    private void send(String to, String subject, String html) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "from", fromAddress,
                    "to", List.of(to),
                    "subject", subject,
                    "html", html,
                    "headers", Map.of(),
                    "tags", List.of(),
                    "click_tracking", false,
                    "open_tracking", false
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildVerifyEmailHtml(String email, String link) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px">
                  <h2 style="color:#6366f1">AffKit Email Verification</h2>
                  <p>Hello <b>%s</b>,</p>
                  <p>Click the button below to verify your email:</p>
                  <a href="%s"
                     style="display:inline-block;background:#6366f1;color:white;
                            padding:12px 28px;border-radius:6px;text-decoration:none;
                            font-weight:bold;margin:16px 0">
                    Verify Email
                  </a>
                  <p style="color:#888;font-size:13px">Link expires in 24 hours.</p>
                </div>
                """.formatted(email, link);
    }

    private String buildResetPasswordHtml(String email, String link) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px">
                  <h2 style="color:#6366f1">AffKit Password Reset</h2>
                  <p>Hello <b>%s</b>,</p>
                  <p>Click the button below to reset your password:</p>
                  <a href="%s"
                     style="display:inline-block;background:#6366f1;color:white;
                            padding:12px 28px;border-radius:6px;text-decoration:none;
                            font-weight:bold;margin:16px 0">
                    Reset Password
                  </a>
                  <p style="color:#888;font-size:13px">Link expires in 1 hour.</p>
                </div>
                """.formatted(email, link);
    }
}