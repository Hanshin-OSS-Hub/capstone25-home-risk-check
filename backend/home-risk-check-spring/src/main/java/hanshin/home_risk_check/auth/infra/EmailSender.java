package hanshin.home_risk_check.auth.infra;

/*
 * 메일 발송 추상화.
 * 구현체: SmtpEmailSender. 향후 외부 메일 API(SES/SendGrid SDK 등)로 교체 가능.
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
