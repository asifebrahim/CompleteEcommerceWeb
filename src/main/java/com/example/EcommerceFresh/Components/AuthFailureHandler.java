package com.example.EcommerceFresh.Components;

import com.example.EcommerceFresh.RedisService.RedisService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthFailureHandler implements AuthenticationFailureHandler {
    private RedisService redisService;
    public AuthFailureHandler(RedisService redisService) {
        this.redisService = redisService;
    }
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)throws IOException, ServletException{
        String email=request.getParameter("email");
        System.out.println("Auth is here");
        if(email!=null && !email.isEmpty()){
            boolean temp=redisService.isAllowed(email,2,60L);
            if(temp==false) response.sendRedirect("/register");
            else{
                System.out.println("This is begin called Always");
                response.sendRedirect("/login");
            }
        }

    }
}
