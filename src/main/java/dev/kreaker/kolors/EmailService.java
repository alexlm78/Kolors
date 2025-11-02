package dev.kreaker.kolors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Restablecimiento de Contraseña - Kolors");
        message.setText(buildPasswordResetEmailBody(resetToken));

        mailSender.send(message);
    }

    /**
     * Send welcome email to new users
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Bienvenido a Kolors");
        message.setText(buildWelcomeEmailBody(username));

        mailSender.send(message);
    }

    /**
     * Build password reset email body
     */
    private String buildPasswordResetEmailBody(String resetToken) {
        return "Hola,\n\n" +
               "Has solicitado restablecer tu contraseña para tu cuenta en Kolors.\n\n" +
               "Para restablecer tu contraseña, haz clic en el siguiente enlace:\n" +
               "http://localhost:8080/auth/reset-password?token=" + resetToken + "\n\n" +
               "Este enlace expirará en 24 horas.\n\n" +
               "Si no solicitaste este restablecimiento, ignora este email.\n\n" +
               "Saludos,\n" +
               "El equipo de Kolors";
    }

    /**
     * Build welcome email body
     */
    private String buildWelcomeEmailBody(String username) {
        return "Hola " + username + ",\n\n" +
               "¡Bienvenido a Kolors! Tu cuenta ha sido creada exitosamente.\n\n" +
               "Ya puedes iniciar sesión y comenzar a crear tus combinaciones de colores.\n\n" +
               "Saludos,\n" +
               "El equipo de Kolors";
    }
}
