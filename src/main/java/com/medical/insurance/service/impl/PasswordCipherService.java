package com.medical.insurance.service.impl;

import com.medical.insurance.exception.AuthValidationException;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordCipherService {
    private static final long CHALLENGE_LIFETIME_SECONDS = 120;
    private final KeyPair keyPair;
    private final Map<String,Challenge> challenges = new ConcurrentHashMap<>();
    private final boolean allowPlaintext;

    public PasswordCipherService(@Value("${app.auth.allow-plaintext-password:false}") boolean allowPlaintext) {
        this.allowPlaintext = allowPlaintext;
        try {
            KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair=generator.generateKeyPair();
         } catch (Exception exception) {
            throw new IllegalStateException("无法初始化密码传输密钥",exception);
        }
    }

    public Map<String,Object> challenge(HttpServletRequest request) {
        challenges.entrySet().removeIf(entry->entry.getValue().expiresAt.isBefore(Instant.now()));
        String id=UUID.randomUUID().toString();
        Instant expiresAt=Instant.now().plusSeconds(CHALLENGE_LIFETIME_SECONDS);
        challenges.put(id,new Challenge(expiresAt,clientIp(request)));
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("challengeId",id);
        result.put("publicKey",Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        result.put("algorithm","RSA-OAEP-256");
        result.put("expiresAt",expiresAt.toString());
        return result;
    }

    public String decrypt(String challengeId,String encryptedValue,String plaintextFallback,HttpServletRequest request) {
         if(encryptedValue==null||encryptedValue.trim().isEmpty()) {
            if(allowPlaintext&&plaintextFallback!=null)return plaintextFallback;
            throw new AuthValidationException("密码必须使用加密通道提交");
        }
        Challenge challenge=challenges.remove(challengeId);
        if(challenge==null||challenge.expiresAt.isBefore(Instant.now())||!challenge.clientIp.equals(clientIp(request)))
            throw new AuthValidationException("密码加密挑战已失效，请重新提交");
        try {
            Cipher cipher=Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE,keyPair.getPrivate(),new OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT));
            String decoded=new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)),StandardCharsets.UTF_8);
            String prefix=challengeId+'\n';
            if(!decoded.startsWith(prefix))throw new AuthValidationException("密码密文与当前挑战不匹配");
            return decoded.substring(prefix.length());
         } catch (AuthValidationException exception) {
            throw exception;
         } catch (Exception exception) {
            throw new AuthValidationException("密码密文无法解密，请重新提交");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded=request.getHeader("X-Forwarded-For");
        return forwarded==null||forwarded.trim().isEmpty()?request.getRemoteAddr():forwarded.split(",")[0].trim();
    }

    private static final class Challenge {
        private final Instant expiresAt;private final String clientIp;
         private Challenge(Instant expiresAt,String clientIp){this.expiresAt=expiresAt;this.clientIp=clientIp;}
    }
}
