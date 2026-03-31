package com.salygin.ssubench.configuration;

import me.paulschwarz.springdotenv.DotenvConfig;
import me.paulschwarz.springdotenv.DotenvPropertySource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.salygin")
@PropertySource("classpath:application.yaml")
public class AppConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer placeholder = new PropertySourcesPlaceholderConfigurer();

        MutablePropertySources propertySources = new MutablePropertySources();

        DotenvPropertySource envSource = new DotenvPropertySource(DotenvConfig.defaults());
        propertySources.addLast(envSource);

        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application.yaml"));

        Properties yamlProperties = yamlFactory.getObject();
        if (yamlProperties != null && !yamlProperties.isEmpty()) {
            propertySources.addLast(new PropertiesPropertySource("yamlSource", yamlProperties));
        }

        placeholder.setPropertySources(propertySources);
        return placeholder;
    }
}

