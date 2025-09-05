package com.example.EcommerceFresh.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class ContactController {

    private final JavaMailSender mailSender;
    private final String SUPPORT_EMAIL = "jnesa38@gmail.com";

    @Autowired
    public ContactController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @GetMapping({"/contact", "/Contact"})
    public String showContactPage() {
        return "Contact";
    }

    @GetMapping("/help")
    public String showHelpPage() {
        return "redirect:/contact";
    }

    @PostMapping("/contact/send")
    public String sendContactEmail(
            @RequestParam(name = "subject", required = false) String subject,
            @RequestParam(name = "details", required = false) String details,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "txnId", required = false) String txnId,
            @RequestParam(name = "issueDate", required = false) String issueDate,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        // Basic server-side validation
        if (subject == null || subject.trim().isEmpty() || details == null || details.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Subject and details are required.");
            return "redirect:/contact";
        }

        String senderEmail = (principal != null) ? principal.getName() : null;
        if (senderEmail == null || senderEmail.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to send a support request.");
            return "redirect:/login";
        }

        // Compose email body
        StringBuilder body = new StringBuilder();
        body.append("Support request from user: ").append(senderEmail).append("\n\n");
        if (name != null && !name.trim().isEmpty()) {
            body.append("Name: ").append(name.trim()).append("\n");
        }
        if (txnId != null && !txnId.trim().isEmpty()) {
            body.append("Transaction ID: ").append(txnId.trim()).append("\n");
        }
        if (issueDate != null && !issueDate.trim().isEmpty()) {
            try {
                LocalDate dt = LocalDate.parse(issueDate);
                body.append("Date related: ").append(dt.format(DateTimeFormatter.ISO_DATE)).append("\n");
            } catch (Exception ex) {
                body.append("Date related: ").append(issueDate).append("\n");
            }
        }
        body.append("\nMessage:\n").append(details.trim()).append("\n\n");
        body.append("--\nThis message sent from EcommerceFresh support form.");

        // Attempt to send
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            // Set From as the user's email; some SMTP providers may override this
            helper.setFrom(senderEmail);
            helper.setReplyTo(senderEmail);
            helper.setTo(SUPPORT_EMAIL);
            helper.setSubject("[Support] " + subject.trim());
            helper.setText(body.toString());
            mailSender.send(message);

            redirectAttributes.addFlashAttribute("success", "Support request sent successfully. Our team will contact you soon.");
            return "redirect:/contact";
        } catch (MessagingException mex) {
            mex.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to send email: " + mex.getMessage());
            return "redirect:/contact";
        } catch (Exception ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while sending your request.");
            return "redirect:/contact";
        }
    }
}
