package com.salygin.ssubench.configuration;

import com.salygin.ssubench.middleware.RequestContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebFilterConfiguration {

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterBean() {
        FilterRegistrationBean<RequestContextFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new RequestContextFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return bean;
    }
}
