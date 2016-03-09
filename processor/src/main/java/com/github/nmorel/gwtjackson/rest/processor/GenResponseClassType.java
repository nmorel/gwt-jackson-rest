package com.github.nmorel.gwtjackson.rest.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( {ElementType.METHOD} )
@Documented
@Retention( RetentionPolicy.SOURCE )
public @interface GenResponseClassType {

    Class<?> value();

}
