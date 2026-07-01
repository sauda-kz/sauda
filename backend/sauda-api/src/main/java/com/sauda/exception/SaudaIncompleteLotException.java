package com.sauda.exception;

import com.sauda.dto.lot.IncompleteLotWarningResponse;
import lombok.Getter;

@Getter
public class SaudaIncompleteLotException extends RuntimeException {

    private final IncompleteLotWarningResponse warning;

    public SaudaIncompleteLotException(IncompleteLotWarningResponse warning) {
        super(warning.message());
        this.warning = warning;
    }
}
