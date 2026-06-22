package com.sauda.domain.enums;

/**
 * Stable role codes seeded in {@code V2__init.sql}. Must match {@code role.code} in the database.
 */
public enum RoleCode {
    platform_admin,
    buyer,
    buyer_approver,
    distributor_manager,
    distributor_viewer
}
