package com.libromagico.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Async
    public void enviarResetPassword(String email, String nombre, String resetLink) {
        if (!mailEnabled) {
            log.info("Email deshabilitado. Reset token para {}: {}", email, resetLink);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("LibroMágico — Recuperación de contraseña");
            helper.setText(buildResetHtml(nombre, resetLink), true);

            mailSender.send(message);
            log.info("Email de recuperación enviado a {}", email);
        } catch (MessagingException e) {
            log.error("Error al enviar email de recuperación a {}: {}", email, e.getMessage());
        }
    }

    private String buildResetHtml(String nombre, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 480px; margin: 0 auto;">
                    <div style="background: #92400e; padding: 24px; text-align: center;">
                        <h1 style="color: #fff; margin: 0;">LibroMágico</h1>
                    </div>
                    <div style="padding: 32px 24px; background: #fff;">
                        <h2 style="color: #92400e;">Recuperá tu contraseña</h2>
                        <p>Hola <strong>%s</strong>, recibimos una solicitud para restablecer tu contraseña.</p>
                        <p>Hacé clic en el botón de abajo para crear una nueva contraseña. Este enlace vence en 1 hora.</p>
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background: #92400e; color: #fff; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-size: 16px; display: inline-block;">Restablecer contraseña</a>
                        </div>
                        <p style="color: #6b7280; font-size: 14px;">Si no solicitaste este cambio, ignorá este mensaje. Tu contraseña actual seguirá funcionando.</p>
                    </div>
                </body>
                </html>
                """.formatted(nombre, resetLink);
    }

    @Async
    public void notificarDevolucionTardia(String email, String tituloLibro, BigDecimal monto) {
        if (!mailEnabled) {
            log.info("Email deshabilitado. Notificación pendiente para {}: devolución tardía de '{}', multa ${}",
                    email, tituloLibro, monto);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("LibroMágico — Devolución tardía: " + tituloLibro);
            helper.setText(buildHtml(tituloLibro, monto), true);

            mailSender.send(message);
            log.info("Email enviado a {}: devolución tardía de '{}', multa ${}", email, tituloLibro, monto);
        } catch (MessagingException e) {
            log.error("Error al enviar email a {}: {}", email, e.getMessage());
        }
    }

    private String buildHtml(String tituloLibro, BigDecimal monto) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 480px; margin: 0 auto;">
                    <div style="background: #7c3aed; padding: 24px; text-align: center;">
                        <h1 style="color: #fff; margin: 0;">LibroMágico</h1>
                    </div>
                    <div style="padding: 32px 24px; background: #fff;">
                        <h2 style="color: #7c3aed;">Devolución tardía</h2>
                        <p>El libro <strong>%s</strong> fue devuelto fuera del plazo establecido.</p>
                        <div style="background: #fef3c7; border: 1px solid #f59e0b; border-radius: 8px; padding: 16px; margin: 24px 0; text-align: center;">
                            <p style="margin: 0; color: #92400e;">Multa generada</p>
                            <p style="font-size: 28px; font-weight: bold; color: #dc2626; margin: 8px 0;">$%.2f</p>
                        </div>
                        <p>Acercate a la biblioteca para regularizar tu situación.</p>
                    </div>
                </body>
                </html>
                """.formatted(tituloLibro, monto);
    }
}
