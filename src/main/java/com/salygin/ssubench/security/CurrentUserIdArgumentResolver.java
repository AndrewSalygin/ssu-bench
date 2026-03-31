package com.salygin.ssubench.security;

import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter param) {
        return param.hasParameterAnnotation(CurrentUserId.class)
                && UUID.class.isAssignableFrom(param.getParameterType());
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter param,
            ModelAndViewContainer container,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof JwtPrincipal jwt) {
            return jwt.id();
        }

        throw new AuthenticationCredentialsNotFoundException("Authentication required.");
    }
}
