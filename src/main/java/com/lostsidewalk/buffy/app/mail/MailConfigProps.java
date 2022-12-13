package com.lostsidewalk.buffy.app.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "mail.config")
public class MailConfigProps {

    private String pwResetEmailSender;

    private String pwResetEmailSubject;

    private String pwResetEmailUrlTemplate;

    private String pwResetEmailBodyTemplate;

    private String verificationEmailSender;

    private String verificationEmailSubject;

    private String verificationEmailUrlTemplate;

    private String verificationEmailBodyTemplate;

    private boolean disabled;

    private boolean logMessages;

    public String getPwResetEmailSender() {
        return pwResetEmailSender;
    }

    @SuppressWarnings("unused")
    public void setPwResetEmailSender(String pwResetEmailSender) {
        this.pwResetEmailSender = pwResetEmailSender;
    }

    public String getPwResetEmailSubject() {
        return pwResetEmailSubject;
    }

    @SuppressWarnings("unused")
    public void setPwResetEmailSubject(String pwResetEmailSubject) {
        this.pwResetEmailSubject = pwResetEmailSubject;
    }

    public String getPwResetEmailUrlTemplate() {
        return pwResetEmailUrlTemplate;
    }

    @SuppressWarnings("unused")
    public void setPwResetEmailUrlTemplate(String pwResetEmailUrlTemplate) {
        this.pwResetEmailUrlTemplate = pwResetEmailUrlTemplate;
    }

    public String getPwResetEmailBodyTemplate() {
        return pwResetEmailBodyTemplate;
    }

    @SuppressWarnings("unused")
    public void setPwResetEmailBodyTemplate(String pwResetEmailBodyTemplate) {
        this.pwResetEmailBodyTemplate = pwResetEmailBodyTemplate;
    }

    public String getVerificationEmailSender() {
        return verificationEmailSender;
    }

    @SuppressWarnings("unused")
    public void setVerificationEmailSender(String verificationEmailSender) {
        this.verificationEmailSender = verificationEmailSender;
    }

    public String getVerificationEmailSubject() {
        return verificationEmailSubject;
    }

    @SuppressWarnings("unused")
    public void setVerificationEmailSubject(String verificationEmailSubject) {
        this.verificationEmailSubject = verificationEmailSubject;
    }

    public String getVerificationEmailUrlTemplate() {
        return verificationEmailUrlTemplate;
    }

    @SuppressWarnings("unused")
    public void setVerificationEmailUrlTemplate(String verificationEmailUrlTemplate) {
        this.verificationEmailUrlTemplate = verificationEmailUrlTemplate;
    }

    public String getVerificationEmailBodyTemplate() {
        return verificationEmailBodyTemplate;
    }

    @SuppressWarnings("unused")
    public void setVerificationEmailBodyTemplate(String verificationEmailBodyTemplate) {
        this.verificationEmailBodyTemplate = verificationEmailBodyTemplate;
    }

    public boolean getDisabled() {
        return disabled;
    }

    @SuppressWarnings("unused")
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean getLogMessages() {
        return logMessages;
    }

    @SuppressWarnings("unused")
    public void setLogMessages(boolean logMessages) {
        this.logMessages = logMessages;
    }
}
