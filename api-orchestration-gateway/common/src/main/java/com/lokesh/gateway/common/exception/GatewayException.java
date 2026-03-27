package com.lokesh.gateway.common.exception;

import lombok.Getter;

@Getter
public class GatewayException extends RuntimeException {

    private final String errorCode;

    public GatewayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GatewayException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
