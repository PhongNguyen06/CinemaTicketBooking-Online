package com.cinema.server;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiErrorHandler {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String,String>> status(ResponseStatusException ex) {
  String reason = ex.getReason()==null?"Request rejected":ex.getReason();
  return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",reason));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException ex) {
  return ResponseEntity.badRequest().body(Map.of("error","Invalid request data"));
 }
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
 public ResponseEntity<Map<String,String>> duplicate(org.springframework.dao.DuplicateKeyException ex) {
  return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","Duplicate value"));
 }
 @ExceptionHandler(org.springframework.dao.DataAccessException.class)
 public ResponseEntity<Map<String,String>> db(org.springframework.dao.DataAccessException ex) {
  return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","Database operation failed; check related records"));
 }
}
