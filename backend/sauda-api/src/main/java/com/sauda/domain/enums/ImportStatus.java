package com.sauda.domain.enums;

public enum ImportStatus {
    pending,
    processing,
    parsed,
    parsed_with_errors,
    awaiting_approval,
    approved,
    rejected,
    applied,
    failed,

    /** @deprecated legacy V2 baseline value, unused by SAUDA-008. */
    @Deprecated
    running,
    /** @deprecated legacy V2 baseline value, unused by SAUDA-008. */
    @Deprecated
    success,
    /** @deprecated legacy V2 baseline value, unused by SAUDA-008. */
    @Deprecated
    partial
}
