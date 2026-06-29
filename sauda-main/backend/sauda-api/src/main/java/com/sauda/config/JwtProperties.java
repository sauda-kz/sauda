package com.sauda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sauda.jwt")
public record JwtProperties(String secret, long expirationMs, long refreshExpirationMs) {}
