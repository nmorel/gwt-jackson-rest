package com.github.nmorel.gwtjackson.rest.api;

/**
 */
public class RestException extends RuntimeException {

    public RestException(String message) {
        super(message);
    }

    public RestException(Throwable cause) {
        super(cause);
    }

}
