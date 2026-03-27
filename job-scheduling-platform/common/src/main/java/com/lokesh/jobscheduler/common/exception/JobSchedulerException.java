package com.lokesh.jobscheduler.common.exception;

import lombok.Getter;

@Getter
public class JobSchedulerException extends RuntimeException {

    private final String errorCode;

    public JobSchedulerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public JobSchedulerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
