package com.cinema.server;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PasswordHasherTest {
 @Test void stage13FormatCompatible() {
  String value=PasswordHasher.hash("MyStrongTestPassword!123");
  assertTrue(value.startsWith("pbkdf2_sha256$"));
  assertTrue(PasswordHasher.verify("MyStrongTestPassword!123",value));
  assertFalse(PasswordHasher.verify("invalid",value));
 }
}
