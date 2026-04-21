package com.res.hms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record AppBootstrapProperties(String adminUsername, String adminPassword) {}
