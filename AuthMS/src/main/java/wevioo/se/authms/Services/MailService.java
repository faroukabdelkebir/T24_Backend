package wevioo.se.authms.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String to, String username,
                                       String resetLink, int expirationMinutes) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Reset your password – DataBridge Banking");
            helper.setText(buildResetHtml(username, resetLink, expirationMinutes), true);

            mailSender.send(msg);
            log.info("Password reset email sent to: {}", to);
        } catch (MessagingException ex) {
            log.error("Failed to send password reset email to {}: {}", to, ex.getMessage());
        }
    }

    @Async
    public void sendPasswordChangedNotification(String to, String username) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Your password has been changed – DataBridge Banking");
            helper.setText(buildChangedHtml(username), true);

            mailSender.send(msg);
        } catch (MessagingException ex) {
            log.error("Failed to send password-changed notification to {}: {}", to, ex.getMessage());
        }
    }


    private String buildResetHtml(String username, String resetLink, int expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
              <div style="background:#1a3c5e;padding:24px;text-align:center;">
                <h1 style="color:#fff;margin:0;font-size:22px;">DataBridge Banking</h1>
              </div>
              <div style="padding:32px;border:1px solid #e0e0e0;">
                <h2 style="color:#1a3c5e;">Password Reset Request</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>We received a request to reset your password. Click the button below to proceed.</p>
                <div style="text-align:center;margin:32px 0;">
                  <a href="%s"
                     style="background:#1a3c5e;color:#fff;padding:12px 28px;
                            border-radius:6px;text-decoration:none;font-size:16px;font-weight:bold;">
                    Reset Password
                  </a>
                </div>
                <p style="color:#666;font-size:13px;">
                  This link expires in <strong>%d minutes</strong>.
                  If you did not request a password reset, please ignore this email.
                </p>
                <p style="color:#666;font-size:13px;">
                  Or copy this URL into your browser:<br/>
                  <a href="%s" style="color:#1a3c5e;">%s</a>
                </p>
              </div>
              <div style="padding:16px;text-align:center;color:#999;font-size:12px;">
                © 2026 DataBridge Banking. This is an automated message.
              </div>
            </body>
            </html>
            """.formatted(username, resetLink, expirationMinutes, resetLink, resetLink);
    }

    private String buildChangedHtml(String username) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
              <div style="background:#1a3c5e;padding:24px;text-align:center;">
                <h1 style="color:#fff;margin:0;font-size:22px;">DataBridge Banking</h1>
              </div>
              <div style="padding:32px;border:1px solid #e0e0e0;">
                <h2 style="color:#1a3c5e;">Password Changed</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your password was successfully changed. If you did not make this change,
                   please contact our support team immediately.</p>
              </div>
            </body>
            </html>
            """.formatted(username);
    }
}

