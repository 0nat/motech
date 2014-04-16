package org.motechproject.email.service.impl;

import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.email.domain.EmailRecord;
import org.motechproject.email.service.EmailAuditService;
import org.motechproject.email.service.EmailRecordSearchCriteria;
import org.motechproject.email.service.EmailRecordService;
import org.motechproject.server.config.SettingsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The <code>EmailAuditServiceImpl</code> class provides API for everything connected with logging e-mails
 * and searching through them
 */

@Service("emailAuditService")
public class EmailAuditServiceImpl implements EmailAuditService {

    private static final String EMAIL_LOG_BODY = "mail.log.body";
    private static final String EMAIL_LOG_ADDRESS = "mail.log.address";
    private static final String EMAIL_LOG_SUBJECT = "mail.log.subject";

    private static final String FALSE = "false";

    ///private AllEmailRecords allEmailRecords;
    private SettingsFacade settings;
    private EmailRecordService emailRecordService;

    @Autowired
    public void setSettings(SettingsFacade settings) {
        this.settings = settings;
    }

    @Autowired
    public void setEmailRecordService(EmailRecordService emailRecordService) {
        this.emailRecordService = emailRecordService;
    }

    @Override
    public void log(EmailRecord emailRecord) {
        if (FALSE.equals(settings.getProperty(EMAIL_LOG_BODY))) {
            emailRecord.setMessage("");
        }

        if (FALSE.equals(settings.getProperty(EMAIL_LOG_ADDRESS))) {
            emailRecord.setFromAddress("");
            emailRecord.setToAddress("");
        }

        if (FALSE.equals(settings.getProperty(EMAIL_LOG_SUBJECT))) {
            emailRecord.setSubject("");
        }

        //allEmailRecords.add(emailRecord);
    }

    @Override
    public List<EmailRecord> findAllEmailRecords() {
        return emailRecordService.retrieveAll();
    }

    @Override
    public void delete(EmailRecord emailRecord) {
        //allEmailRecords.remove(emailRecord);
    }

    @Override
    public List<EmailRecord> findEmailRecords(EmailRecordSearchCriteria criteria) {
        EmailRecord r = new EmailRecord();
        r.setMessage("Msg " + DateUtil.now());
        emailRecordService.create(r);
        return emailRecordService.retrieveAll();
    }
}
