package com.sauda.domain.enums;

public enum UserRole {
    platform_admin,
    buyer,
    distributor_manager;

    public String toDatabaseValue() {
        return switch (this) {
            case platform_admin -> "platform_admin";
            case buyer -> "buyer";
            case distributor_manager -> "distributor.manager";
        };
    }

    public static UserRole fromDatabaseValue(String value) {
        return switch (value) {
            case "platform_admin" -> platform_admin;
            case "buyer" -> buyer;
            case "distributor.manager" -> distributor_manager;
            default -> throw new IllegalArgumentException("Unknown user role: " + value);
        };
    }
}
