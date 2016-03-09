package com.github.nmorel.gwtjackson.rest.processor;

import javax.lang.model.element.ExecutableElement;

public class MissingGenResponseClassTypeException extends RuntimeException {

    private final ExecutableElement method;

    public MissingGenResponseClassTypeException( ExecutableElement method ) {
        this.method = method;
    }

    public ExecutableElement getMethod() {
        return method;
    }
}
