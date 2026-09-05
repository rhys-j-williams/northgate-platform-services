package com.meridian.platform.bedrockadapter.gateway;

public class BedrockUnavailableException extends RuntimeException {

    public BedrockUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public BedrockUnavailableException(String message) {
        super(message);
    }
}
