package com.medical.insurance.service.impl;

import com.medical.insurance.model.PasswordResetMailRequested;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class PasswordResetMailService {
    private static final Logger log=LoggerFactory.getLogger(PasswordResetMailService.class);
    private final ObjectProvider<JavaMailSender> senderProvider;
    private final boolean enabled;
    private final String from;

    public PasswordResetMailService(ObjectProvider<JavaMailSender> senderProvider,@Value("${app.auth.reset-mail-enabled:false}")boolean enabled,@Value("${app.auth.reset-mail-from:no-reply@medical.local}")String from){this.senderProvider=senderProvider;this.enabled=enabled;this.from=from;}

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void send(PasswordResetMailRequested request){if(!enabled)return;JavaMailSender sender=senderProvider.getIfAvailable();if(sender==null){log.warn("密码重置邮件服务尚未配置");return;}long started=System.nanoTime();try{SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(request.address());message.setSubject("医疗保险报销系统密码重置验证码");message.setText("您的密码重置验证码为："+request.code()+"\n验证码10分钟内有效，请勿转发给他人。\n如非本人操作，请忽略本邮件。");sender.send(message);log.info("密码重置邮件已交给SMTP服务器，耗时={}ms",elapsedMillis(started));}catch(Exception exception){log.warn("密码重置邮件后台发送失败，耗时={}ms，原因={}",elapsedMillis(started),exception.getMessage());}}

    private long elapsedMillis(long started){return (System.nanoTime()-started)/1_000_000L;}
}
