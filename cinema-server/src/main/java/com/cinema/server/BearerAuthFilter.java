package com.cinema.server;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class BearerAuthFilter extends OncePerRequestFilter {
 private final AuthSessionStore store;
 public BearerAuthFilter(AuthSessionStore store) {this.store=store;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse resp,FilterChain chain) throws ServletException,IOException {
  String header=req.getHeader("Authorization");
  if(header!=null && header.startsWith("Bearer ")) {
   AuthSessionStore.Session s=store.get(header.substring(7));
   if(s!=null) {
    var authorities=List.of(new SimpleGrantedAuthority("ROLE_"+s.role()));
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(s,null,authorities));
   }
  }
  try {chain.doFilter(req,resp);} finally {SecurityContextHolder.clearContext();}
 }
}
