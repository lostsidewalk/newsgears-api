package com.lostsidewalk.buffy.app.mail;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import com.lostsidewalk.buffy.app.audit.MailException;
import com.lostsidewalk.buffy.app.model.AppToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class MailService {

    @Autowired
    UserDao userDao;

    @Autowired
    MailConfigProps configProps;

    @Autowired
    JavaMailSender mailSender;

    //
    // password reset
    //

    public void sendPasswordResetEmail(String username, AppToken passwordResetToken) throws MailException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String n = user.getUsername();
        String emailAddress = user.getEmailAddress();
        if (isBlank(emailAddress)) {
            throw new MailException("Unable to send password reset email because user has no known email address");
        }
        try {
            generatePasswordResetEmail(n, emailAddress, passwordResetToken);
        } catch (Exception e) {
            throw new MailException("Unable to send password reset email due to: " + e.getMessage());
        }
    }

    private void generatePasswordResetEmail(String username, String emailAddress, AppToken passwordResetToken) {
        String from = configProps.getPwResetEmailSender();
        String subject = configProps.getPwResetEmailSubject();
        String pwResetUrl = String.format(configProps.getPwResetEmailUrlTemplate(), passwordResetToken.authToken);
        String body = String.format(configProps.getPwResetEmailBodyTemplate(), username, pwResetUrl);
        sendSimpleMessage(from, emailAddress, subject, body);
    }

    //
    // verification
    //

    public void sendVerificationEmail(String username, AppToken verificationToken) throws MailException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String n = user.getUsername();
        String emailAddress = user.getEmailAddress();
        if (isBlank(emailAddress)) {
            throw new MailException("Unable to send verification email because user has no known email address");
        }
        generateVerificationEmail(n, emailAddress, verificationToken);
    }

    private void generateVerificationEmail(String username, String emailAddress, AppToken verificationToken) {
        String from = configProps.getVerificationEmailSender();
        String subject = configProps.getVerificationEmailSubject();
        String verificationUrl = String.format(configProps.getVerificationEmailUrlTemplate(), verificationToken.authToken);
        String body = String.format(configProps.getVerificationEmailBodyTemplate(), username, verificationUrl);
        sendSimpleMessage(from, emailAddress, subject, body);
    }

    //
    //
    //

    public void sendSimpleMessage(String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        if (configProps.getLogMessages()) {
            log.info("Sending mail message={}", message);
        }

        if (configProps.getDisabled()) {
            log.warn("Mail sender is administratively disabled");
            return;
        }

        mailSender.send(message);
    }
}
