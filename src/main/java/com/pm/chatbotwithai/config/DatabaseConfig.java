package com.pm.chatbotwithai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "com.pm.chatbotwithai.repository")
@ConfigurationProperties(prefix = "chatbot.db")
public class DatabaseConfig {
    private String url = "jdbc:h2:mem:chatbotdb";
    private String username = "sa";
    private String password = "";
    private String driverClassName = "org.h2.Driver";

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    // Getters and setters for configuration properties
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
}
