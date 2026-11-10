package com.cinema.server;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
/** Phase 14A: in-memory sessions. One API instance, sessions lost on restart. */
@Component
public class AuthSessionStore {
 public record Session(long userId, String username, String role, Long customerId, Instant expiresAt) {}
 private final Map<String,Session> sessions = new ConcurrentHashMap<>();
 private final SecureRandom random = new SecureRandom();
 public String create(long id,String username,String role,Long customerId) {
  byte[] bytes = new byte[32]; random.nextBytes(bytes);
  String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  sessions.put(token,new Session(id,username,role,customerId,Instant.now().plusSeconds(8*3600)));
  return token;
 }
 public Session get(String token) {
  Session s=sessions.get(token);
  if(s!=null && s.expiresAt().isBefore(Instant.now())) {sessions.remove(token);return null;}
  return s;
 }
 public void revoke(String token) {sessions.remove(token);}
}
