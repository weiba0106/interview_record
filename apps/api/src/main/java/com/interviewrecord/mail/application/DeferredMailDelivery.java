package com.interviewrecord.mail.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.core.task.TaskExecutor;

/** Keeps SMTP latency out of public, account-enumeration-sensitive requests. */
@Component
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class DeferredMailDelivery {
    private static final Logger log = LoggerFactory.getLogger(DeferredMailDelivery.class);
    private final MailGateway mail;
    private final TaskExecutor mailDeliveryExecutor;

    public DeferredMailDelivery(MailGateway mail, @Qualifier("mailDeliveryExecutor") TaskExecutor mailDeliveryExecutor) {
        this.mail = mail;
        this.mailDeliveryExecutor = mailDeliveryExecutor;
    }

    public void sendVerificationEmail(String email, String rawToken) {
        submit(() -> mail.sendVerificationEmail(email, rawToken), "VERIFICATION_DELIVERY_FAILED");
    }

    public void sendPasswordResetEmail(String email, String rawToken) {
        submit(() -> mail.sendPasswordResetEmail(email, rawToken), "PASSWORD_RESET_DELIVERY_FAILED");
    }

    private void submit(Runnable delivery, String errorCode) {
        try {
            mailDeliveryExecutor.execute(() -> {
                try { delivery.run(); }
                catch (RuntimeException exception) {
                    log.atWarn().addKeyValue("error_code", errorCode).log("mail_delivery_failed");
                }
            });
        } catch (RuntimeException exception) {
            log.atWarn().addKeyValue("error_code", errorCode).log("mail_delivery_rejected");
        }
    }

    @Configuration
    static class ExecutorConfiguration {
        @Bean(name = "mailDeliveryExecutor")
        @Profile("test")
        TaskExecutor synchronousMailDeliveryExecutor() { return new SyncTaskExecutor(); }

        @Bean(name = "mailDeliveryExecutor")
        @Profile("!test")
        TaskExecutor asynchronousMailDeliveryExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(4);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("mail-delivery-");
            executor.initialize();
            return executor;
        }
    }
}
