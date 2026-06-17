package bodega_system.service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetCode(String to, String code) {

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom("eliasdev2026@gmail.com","EliasDev");
            helper.setSubject("Recuperación de contraseña - EliasDev");

            String html = """
                <div style="
                    font-family: Arial, sans-serif;
                    background: #f3f4f6;
                    padding: 40px;
                ">
                    <div style="
                        max-width: 560px;
                        margin: auto;
                        background: #ffffff;
                        border-radius: 18px;
                        padding: 32px;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.08);
                    ">

                        <h1 style="
                            margin: 0 0 10px;
                            color: #111827;
                            font-size: 28px;
                        ">
                            🔐 EliasDev
                        </h1>

                        <h2 style="
                            margin: 0 0 20px;
                            color: #2563eb;
                            font-size: 22px;
                        ">
                            Recuperación de contraseña
                        </h2>

                        <p style="
                            color: #374151;
                            font-size: 16px;
                            line-height: 1.5;
                        ">
                            Recibimos una solicitud para restablecer la contraseña de tu cuenta.
                        </p>

                        <p style="
                            color: #374151;
                            font-size: 16px;
                            line-height: 1.5;
                        ">
                            Usá el siguiente código para continuar:
                        </p>

                        <div style="
                            margin: 28px 0;
                            background: #eff6ff;
                            border: 1px solid #bfdbfe;
                            color: #1d4ed8;
                            font-size: 36px;
                            font-weight: 800;
                            letter-spacing: 8px;
                            text-align: center;
                            padding: 22px;
                            border-radius: 14px;
                        ">
                            %s
                        </div>

                        <p style="
                            color: #dc2626;
                            font-size: 14px;
                            font-weight: 600;
                        ">
                            Este código vence en 5 minutos.
                        </p>

                        <hr style="
                            border: none;
                            border-top: 1px solid #e5e7eb;
                            margin: 26px 0;
                        ">

                        <p style="
                            color: #6b7280;
                            font-size: 13px;
                            line-height: 1.5;
                        ">
                            Si no solicitaste este cambio, podés ignorar este correo.
                        </p>

                        <p style="
                            color: #6b7280;
                            font-size: 13px;
                            margin-top: 20px;
                        ">
                            Equipo EliasDev
                        </p>

                    </div>
                </div>
                """.formatted(code);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                "Error enviando email: " + e.getMessage()
            );
        }
    }
}