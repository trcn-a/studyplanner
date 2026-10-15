package ua.studyplanner.googlecalendar.integration;

import org.springframework.stereotype.Component;
import ua.studyplanner.googlecalendar.config.GoogleCalendarConfig;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class TokenCipher {
 private final GoogleCalendarConfig config;
 public TokenCipher(GoogleCalendarConfig config) { this.config=config; }
 public String encrypt(String value) { return transform(value,true); }
 public String decrypt(String value) { return transform(value,false); }
 private String transform(String value, boolean encrypt) {
  try {
   byte[] iv=new byte[12]; byte[] data;
   if(encrypt) { new SecureRandom().nextBytes(iv); data=value.getBytes(StandardCharsets.UTF_8); }
   else { byte[] packed=Base64.getDecoder().decode(value);iv=Arrays.copyOfRange(packed,0,12);data=Arrays.copyOfRange(packed,12,packed.length); }
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(encrypt?Cipher.ENCRYPT_MODE:Cipher.DECRYPT_MODE,new SecretKeySpec(Base64.getDecoder().decode(config.encryptionKey()),"AES"),new GCMParameterSpec(128,iv));
   byte[] result=cipher.doFinal(data);
   if(!encrypt) return new String(result,StandardCharsets.UTF_8);
   byte[] packed=Arrays.copyOf(iv,iv.length+result.length);System.arraycopy(result,0,packed,iv.length,result.length);
   return Base64.getEncoder().encodeToString(packed);
  } catch(Exception e) { throw new IllegalStateException("Не вдалося обробити ключ підключення Google"); }
 }
}
