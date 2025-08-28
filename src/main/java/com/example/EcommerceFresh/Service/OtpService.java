package com.example.EcommerceFresh.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.validMinutes:10}")
    private int otpValidityMinutes;

    public OtpService(JavaMailSender mailSender, BCryptPasswordEncoder passwordEncoder) {
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateNumericOtp() {
        int number = 100000 + secureRandom.nextInt(900000);
        return Integer.toString(number);
    }

    public String hashOtp(String otp) {
        // use BCrypt to hash the OTP for storage; compare with matches()
        return passwordEncoder.encode(otp);
    }

    public boolean verifyOtp(String rawOtp, String otpHash) {
        try {
            return passwordEncoder.matches(rawOtp, otpHash);
        } catch (Exception e) {
            return false;
        }
    }

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your verification code");
        message.setText("Your OTP code is: " + otp + "\nThis code will expire in " + otpValidityMinutes + " minutes.");
        mailSender.send(message);
    }

    public LocalDateTime computeExpiry() {
        return LocalDateTime.now().plusMinutes(otpValidityMinutes);
    }
}
