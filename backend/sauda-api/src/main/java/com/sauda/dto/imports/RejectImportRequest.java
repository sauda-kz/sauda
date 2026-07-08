package com.sauda.dto.imports;

import jakarta.validation.constraints.Size;

public record RejectImportRequest(@Size(max = 500) String reason) {}
