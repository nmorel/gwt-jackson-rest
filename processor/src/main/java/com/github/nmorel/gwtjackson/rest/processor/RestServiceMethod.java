/*
 * Copyright 2015 Nicolas Morel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nmorel.gwtjackson.rest.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

/**
 * @author Nicolas Morel
 */
public class RestServiceMethod {

    private final ExecutableElement method;

    private final AnnotationMirror httpMethodAnnotation;

    private final String url;

    private final VariableElement bodyParamVariable;

    private final TypeMirror returnType;

    public RestServiceMethod( ExecutableElement method, String baseRestUrl, AnnotationMirror httpMethodAnnotation ) {
        this.method = method;
        this.httpMethodAnnotation = httpMethodAnnotation;

        StringBuilder urlBuilder = new StringBuilder( baseRestUrl );
        Path path = method.getAnnotation( Path.class );
        if ( null == path ) {
            this.url = urlBuilder.toString();
        } else {
            if ( !baseRestUrl.endsWith( "/" ) && !path.value().startsWith( "/" ) ) {
                urlBuilder.append( '/' );
            }
            urlBuilder.append( path.value() );
            this.url = urlBuilder.toString();
        }

        VariableElement bodyParamVariable = null;
        for ( VariableElement variable : method.getParameters() ) {
            if ( null == variable.getAnnotation( PathParam.class ) && null == variable.getAnnotation( QueryParam.class )
                    && null == variable.getAnnotation( Context.class ) ) {

                if ( null == bodyParamVariable ) {
                    bodyParamVariable = variable;
                } else {
                    throw new MoreThanOneBodyParamException( method );
                }
            }
        }
        this.bodyParamVariable = bodyParamVariable;

        this.returnType = method.getReturnType();
    }

    public ExecutableElement getMethod() {
        return method;
    }

    public AnnotationMirror getHttpMethodAnnotation() {
        return httpMethodAnnotation;
    }

    public String getUrl() {
        return url;
    }

    public VariableElement getBodyParamVariable() {
        return bodyParamVariable;
    }

    public TypeMirror getReturnType() {
        return returnType;
    }
}
