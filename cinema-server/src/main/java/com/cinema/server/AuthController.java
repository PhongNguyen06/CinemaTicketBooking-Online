package com.cinema.server;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
 public record LoginRequest(@NotBlank String username,@NotBlank String password) {}
 public record LoginResponse(String token,String username,String role,Long customerId) {}
 public record RegisterRequest(
  @NotBlank @Pattern(regexp="[A-Za-z0-9_]{4,20}") String username,
  @NotBlank @Size(min=12,max=128) String password,
  @NotBlank @Size(min=2,max=100) String name,
  @NotBlank @Size(max=255) @Pattern(regexp="^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") String email,
  @NotBlank @Pattern(regexp="0[0-9]{9}") String phone) {}
 public record RegisterResponse(String username, String role) {}
 private static final SecureRandom RANDOM = new SecureRandom();
 private static long newId() {
  // Random positive, high-range IDs avoid changing existing primary keys / foreign keys.
  // TiDB/MySQL PRIMARY KEY enforces uniqueness; highly unlikely collisions are rejected.
  return (1L << 51) | (RANDOM.nextLong() & ((1L << 51) - 1));
 }
 private final JdbcTemplate jdbc;
 private final AuthSessionStore store;
 public AuthController(JdbcTemplate jdbc,AuthSessionStore store){this.jdbc=jdbc;this.store=store;}
 @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) {
  var users=jdbc.query("SELECT id,username,password,role,customer_id FROM users WHERE username=?",
   (rs,n)->new Account(rs.getLong("id"),rs.getString("username"),rs.getString("password"),rs.getString("role"),
    rs.getObject("customer_id")==null?null:rs.getLong("customer_id")),request.username());
  if(users.size()!=1 || !PasswordHasher.verify(request.password(),users.get(0).password()))
   throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid username or password");
  var u=users.get(0);
  if(!"ADMIN".equals(u.role()) && !"USER".equals(u.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account role must be ADMIN or USER; contact administrator");
  return new LoginResponse(store.create(u.id(),u.username(),u.role(),u.customerId()),u.username(),u.role(),u.customerId());
 }
 /** Public, read-only deployment probe: does not reveal accounts or secrets. */
 @GetMapping("/registration-status")
 public Map<String,String> registrationStatus() {
  return Map.of("feature","customer-registration","version","14J-fix1","status","available");
 }
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
  String username=request.username().trim();
  String email=request.email().trim().toLowerCase(Locale.ROOT);
  String phone=request.phone().trim();
  String name=request.name().trim().replaceAll("\\s+"," ");
  if(name.length()<2 || name.length()>100)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Full name must be 2-100 characters");
  Integer usernames=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?",Integer.class,username);
  if(usernames!=null && usernames>0)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Username already exists");
  Integer emails=jdbc.queryForObject("SELECT COUNT(*) FROM customers WHERE LOWER(email)=?",Integer.class,email);
  if(emails!=null && emails>0)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
  Integer phones=jdbc.queryForObject("SELECT COUNT(*) FROM customers WHERE phone=?",Integer.class,phone);
  if(phones!=null && phones>0)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Phone number already exists");
  long customerId=newId();
  long userId=newId();
  // The two INSERTs commit together. Never trust a role supplied by the client.
  try {
   jdbc.update("INSERT INTO customers(id,name,email,phone) VALUES(?,?,?,?)",customerId,name,email,phone);
   jdbc.update("INSERT INTO users(id,username,password,role,customer_id) VALUES(?, ?, ?, 'USER', ?)",
     userId,username,PasswordHasher.hash(request.password()),customerId);
  } catch (DataIntegrityViolationException ex) {
   // Transaction rolls back both inserts on any database constraint violation.
   throw new ResponseStatusException(HttpStatus.CONFLICT,
      "Username, email or phone may already be in use", ex);
  }
  return new RegisterResponse(username,"USER");
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication auth) {
  var s=(AuthSessionStore.Session)auth.getPrincipal();
  return Map.of("id",s.userId(),"username",s.username(),"role",s.role());
 }
 @PostMapping("/logout") public void logout(@RequestHeader("Authorization") String authorization) {
  if(authorization.startsWith("Bearer ")) store.revoke(authorization.substring(7));
 }
 private record Account(long id,String username,String password,String role,Long customerId) {}
}
