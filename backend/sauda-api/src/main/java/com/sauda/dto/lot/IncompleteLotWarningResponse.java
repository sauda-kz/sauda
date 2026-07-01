package com.sauda.dto.lot;

import java.util.List;

public record IncompleteLotWarningResponse(String message, List<String> missingFields) {}
