package com.cinema.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;

/** Broadcast only after a successful mutating HTTP response; @Transactional has committed at this point. */
@Component
public class LiveUpdateInterceptor implements HandlerInterceptor {
    private final LiveUpdates updates;
    public LiveUpdateInterceptor(LiveUpdates updates) {this.updates=updates;}
    @Override public void afterCompletion(HttpServletRequest req,HttpServletResponse res,Object handler,Exception ex) {
        if(ex!=null || res.getStatus()<200 || res.getStatus()>=300) return;
        String method=req.getMethod();
        if(!method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE")) return;
        String path=req.getRequestURI().substring(req.getContextPath().length());
        for(String topic:new String[]{"movies","cinemas","rooms","seats","showtimes","reservations"}) {
            if(path.equals("/api/"+topic) || path.startsWith("/api/"+topic+"/")) {
                updates.changed(topic);
                // Modifying reservations affects availability displayed in the seat selector.
                if(topic.equals("reservations")) updates.changed("seats");
                return;
            }
        }
    }
}
@Configuration
class LiveUpdateMvcConfig implements WebMvcConfigurer {
    private final LiveUpdateInterceptor interceptor;
    LiveUpdateMvcConfig(LiveUpdateInterceptor interceptor) {this.interceptor=interceptor;}
    @Override public void addInterceptors(InterceptorRegistry registry) {registry.addInterceptor(interceptor).addPathPatterns("/api/**");}
}
