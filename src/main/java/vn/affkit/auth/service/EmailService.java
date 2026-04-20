package vn.affkit.auth.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(List.of(to))
                    .subject(subject)
                    .html(html)
                    .build();
            resend.emails().send(params);
            log.info("Email sent to: {}", to);
        } catch (ResendException e) {
            log.warn("ResendException sending to {}: {}", to, e.getMessage());
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