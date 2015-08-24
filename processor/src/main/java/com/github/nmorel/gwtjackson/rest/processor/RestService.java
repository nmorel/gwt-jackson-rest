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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicolas Morel
 */
public class RestService {

    private final TypeElement typeElement;

    private final String packageName;

    private final String builderClassName;

    private final List<RestServiceMethod> methods = new ArrayList<RestServiceMethod>();

    private final Map<ExecutableElement, Exception> methodsInError = new HashMap<ExecutableElement, Exception>();

    private final Set<TypeMirror> returnTypes = new LinkedHashSet<TypeMirror>();

    private final Set<TypeMirror> bodyTypes = new LinkedHashSet<TypeMirror>();

    public RestService( Options options, Element element ) {
        // only types can be annotated with @GenRestService so it's safe to cast into TypeElement
        typeElement = (TypeElement) element;

        if ( null == options.getPackageName() ) {
            Element enclosingElement = typeElement.getEnclosingElement();
            while ( ElementKind.PACKAGE != enclosingElement.getKind() ) {
                enclosingElement = enclosingElement.getEnclosingElement();
            }
            PackageElement packageElement = (PackageElement) enclosingElement;
            packageName = packageElement.getQualifiedName().toString();
        } else {
            packageName = options.getPackageName();
        }

        builderClassName = typeElement.getSimpleName().toString() + "Builder";

        Path path = typeElement.getAnnotation( Path.class );
        String baseRestUrl = path.value();

        for ( ExecutableElement method : ElementFilter.methodsIn( typeElement.getEnclosedElements() ) ) {
            parseMethod( baseRestUrl, method );
        }
    }

    private void parseMethod( String baseRestUrl, ExecutableElement method ) {
        AnnotationMirror httpMethodAnnotation = isRestMethod( method );
        if ( null == httpMethodAnnotation ) {
            // not a rest method
            return;
        }

        RestServiceMethod restServiceMethod;
        try {
            restServiceMethod = new RestServiceMethod( method, baseRestUrl, httpMethodAnnotation );
        } catch ( Exception e ) {
            methodsInError.put( method, e );
            return;
        }

        methods.add( restServiceMethod );

        if ( TypeKind.VOID != restServiceMethod.getReturnType().getKind() ) {
            returnTypes.add( restServiceMethod.getReturnType() );
        }

        if ( null != restServiceMethod.getBodyParamVariable() ) {
            bodyTypes.add( restServiceMethod.getBodyParamVariable().asType() );
        }
    }

    /**
     * Check if the method is a REST method. If the method has a HTTP method annotation like {@link GET} and is not ignored with {@link
     * GenRestIgnore} then it's a REST method.
     *
     * @param method the method to check
     *
     * @return the HTTP method annotation found or null if the method is not a REST method or is ignored
     */
    private AnnotationMirror isRestMethod( ExecutableElement method ) {
        AnnotationMirror httpMethod = null;
        for ( AnnotationMirror m : method.getAnnotationMirrors() ) {
            if ( m.getAnnotationType().toString().equals( GenRestIgnore.class.getName() ) ) {
                return null;
            }

            if ( m.getAnnotationType().toString().equals( GET.class.getName() ) ) {
                httpMethod = m;
            } else if ( m.getAnnotationType().toString().equals( POST.class.getName() ) ) {
                httpMethod = m;
            } else if ( m.getAnnotationType().toString().equals( PUT.class.getName() ) ) {
                httpMethod = m;
            } else if ( m.getAnnotationType().toString().equals( DELETE.class.getName() ) ) {
                httpMethod = m;
            } else if ( m.getAnnotationType().toString().equals( HEAD.class.getName() ) ) {
                httpMethod = m;
            }
        }
        return httpMethod;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getBuilderSimpleClassName() {
        return builderClassName;
    }

    public String getBuilderQualifiedClassName() {
        return packageName + "." + builderClassName;
    }

    public List<RestServiceMethod> getMethods() {
        return methods;
    }

    public Map<ExecutableElement, Exception> getMethodsInError() {
        return methodsInError;
    }

    public Set<TypeMirror> getReturnTypes() {
        return returnTypes;
    }

    public Set<TypeMirror> getBodyTypes() {
        return bodyTypes;
    }
}
