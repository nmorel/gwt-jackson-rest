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

package com.github.nmorel.gwtjackson.rest.annotations;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.nmorel.gwtjackson.client.ObjectReader;
import com.github.nmorel.gwtjackson.client.ObjectWriter;
import com.github.nmorel.gwtjackson.rest.api.RestRequestBuilder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

/**
 * Processor handling type annotated with {@link GenRestService}.
 */
@SupportedAnnotationTypes( "com.github.nmorel.gwtjackson.rest.annotations.GenRestService" )
@SupportedSourceVersion( SourceVersion.RELEASE_7 )
public class RestServiceProcessor extends AbstractProcessor {

    private static final String DEFAULT_PACKAGE_PREFIX = "com.github.nmorel.gwtjackson.rest.gen";

    @Override
    public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        if ( roundEnv.processingOver() || annotations.size() == 0 ) {
            return false;
        }
        for ( Element element : roundEnv.getRootElements() ) {
            if ( isAnnotatedWith( element, GenRestService.class ) && isAnnotatedWith( element, Path.class ) ) {
                try {
                    TypeElement classElement = (TypeElement) element;

                    PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();
                    String packageName = DEFAULT_PACKAGE_PREFIX + "." + packageElement.getQualifiedName();

                    TypeSpec type = generateBuilder( packageName, classElement );

                    JavaFileObject jfo = processingEnv.getFiler().createSourceFile( packageName + "." + type.name );
                    JavaFile file = JavaFile.builder( packageName, type ).build();
                    Writer writer = jfo.openWriter();
                    file.writeTo( writer );
                    writer.close();
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Generate the rest service
     *
     * @param packageName
     * @param classElement the annotated type
     *
     * @throws Exception
     */
    private TypeSpec generateBuilder( String packageName, TypeElement classElement ) throws Exception {
        String className = classElement.getSimpleName().toString() + "Builder";

        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder( className )
                .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                .addJavadoc( "Generated REST service for {@link $L}.\n", classElement.getQualifiedName() )
                .addMethod( MethodSpec.constructorBuilder().addModifiers( Modifier.PRIVATE ).build() );

        AnnotationMirror pathAnnotation = getAnnotationMirror( classElement, Path.class );
        String baseRestUrl = getAnnotationValue( pathAnnotation, "value" ).getValue().toString();

        List<ExecutableElement> methods = new ArrayList<ExecutableElement>();
        List<TypeMirror> returnTypes = new ArrayList<TypeMirror>();
        List<TypeMirror> bodyTypes = new ArrayList<TypeMirror>();

        for ( ExecutableElement method : ElementFilter.methodsIn( classElement.getEnclosedElements() ) ) {
            parseMethod( methods, returnTypes, bodyTypes, method );
        }

        Map<TypeMirror, MethodSpec> writerGetters = buildWriters( packageName, className, typeBuilder, bodyTypes );
        Map<TypeMirror, MethodSpec> readerGetters = buildReaders( packageName, className, typeBuilder, returnTypes );

        for ( ExecutableElement method : methods ) {
            MethodSpec methodSpec = buildMethod( baseRestUrl, writerGetters, readerGetters, method );
            typeBuilder.addMethod( methodSpec );
        }

        return typeBuilder.build();
    }

    private Map<TypeMirror, MethodSpec> buildWriters( String packageName, String className, Builder typeBuilder, List<TypeMirror>
            bodyTypes ) {
        return buildMappers( packageName, className, typeBuilder, bodyTypes, ObjectWriter.class );
    }

    private Map<TypeMirror, MethodSpec> buildReaders( String packageName, String className, Builder typeBuilder, List<TypeMirror>
            returnTypes ) {
        return buildMappers( packageName, className, typeBuilder, returnTypes, ObjectReader.class );
    }

    private Map<TypeMirror, MethodSpec> buildMappers( String packageName, String className, Builder typeBuilder, List<TypeMirror>
            types, Class clazz ) {
        int i = 1;
        Map<TypeMirror, MethodSpec> result = new HashMap<TypeMirror, MethodSpec>();
        for ( TypeMirror type : types ) {
            String mapperName = clazz.getSimpleName() + i++;

            TypeName mapperType = ClassName.get( packageName, className, mapperName );

            TypeSpec innerMapper = TypeSpec.interfaceBuilder( mapperName )
                    .addModifiers( Modifier.STATIC )
                    .addSuperinterface( ParameterizedTypeName.get( ClassName.get( clazz ), ClassName.get( type ) ) )
                    .build();
            typeBuilder.addType( innerMapper );

            FieldSpec mapperField = FieldSpec
                    .builder( mapperType, mapperName.toLowerCase() )
                    .addModifiers( Modifier.PRIVATE, Modifier.STATIC )
                    .build();
            typeBuilder.addField( mapperField );

            MethodSpec mapperGetter = MethodSpec.methodBuilder( "get" + mapperName )
                    .addModifiers( Modifier.PRIVATE, Modifier.STATIC )
                    .returns( mapperType )
                    .beginControlFlow( "if ($N == null)", mapperField )
                    .addStatement( "$N = $T.create($T.class)", mapperField, GWT.class, mapperType )
                    .endControlFlow()
                    .addStatement( "return $N", mapperField )
                    .build();
            typeBuilder.addMethod( mapperGetter );

            result.put( type, mapperGetter );
        }
        return result;
    }

    private void parseMethod( List<ExecutableElement> methods, List<TypeMirror> returnTypes, List<TypeMirror> bodyTypes,
                              ExecutableElement method ) {
        AnnotationMirror httpMethodAnnotation = findHttpMethod( method );
        if ( null == httpMethodAnnotation ) {
            return;
        }

        methods.add( method );

        if ( TypeKind.VOID != method.getReturnType().getKind() ) {
            returnTypes.add( method.getReturnType() );
        }

        VariableElement bodyVariable = findBodyVariable( method );
        if ( null != bodyVariable ) {
            bodyTypes.add( bodyVariable.asType() );
        }
    }

    private VariableElement findBodyVariable( ExecutableElement method ) {
        VariableElement bodyVariable = null;
        for ( VariableElement variable : method.getParameters() ) {
            if ( !isAnnotatedWith( variable, PathParam.class )
                    && !isAnnotatedWith( variable, QueryParam.class ) ) {
                if ( null != bodyVariable ) {
                    // TODO
                    System.out.println( "Cannot have more than one parameter for body" );
                } else {
                    bodyVariable = variable;
                }
            }
        }
        return bodyVariable;
    }

    private MethodSpec buildMethod( String baseRestUrl, Map<TypeMirror, MethodSpec> writerGetters, Map<TypeMirror, MethodSpec>
            readerGetters, ExecutableElement method ) {
        AnnotationMirror httpMethodAnnotation = findHttpMethod( method );

        TypeMirror returnType = method.getReturnType();
        MethodSpec returnTypeReaderGetter = readerGetters.get( returnType );
        TypeName returnTypeName;
        if ( null == returnTypeReaderGetter ) {
            returnTypeName = ClassName.get( Void.class );
        } else {
            returnTypeName = TypeName.get( returnType );
        }

        VariableElement bodyVariable = findBodyVariable( method );
        MethodSpec bodyTypeWriterGetter;
        TypeName bodyTypeName;

        if ( null != bodyVariable ) {
            bodyTypeWriterGetter = writerGetters.get( bodyVariable.asType() );
            bodyTypeName = TypeName.get( bodyVariable.asType() );
        } else {
            bodyTypeWriterGetter = null;
            bodyTypeName = ClassName.get( Void.class );
        }

        TypeName restType = ParameterizedTypeName.get( ClassName.get( RestRequestBuilder.class ), bodyTypeName, returnTypeName );

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder( method.getSimpleName().toString() )
                .addModifiers( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                .returns( restType );

        CodeBlock.Builder initRestBuilder = CodeBlock.builder()
                .add( "new $T()", restType )
                .indent()
                .add( "\n.method($T.$L)", RequestBuilder.class, httpMethodAnnotation.getAnnotationType().asElement().getSimpleName() )
                .add( "\n.url($S)", baseRestUrl );

        if ( null != bodyVariable ) {
            initRestBuilder.add( "\n.body($L)", bodyVariable.getSimpleName() );
        }
        if ( null != bodyTypeWriterGetter ) {
            initRestBuilder.add( "\n.bodyConverter($N())", bodyTypeWriterGetter );
        }

        for ( VariableElement variable : method.getParameters() ) {
            methodSpecBuilder.addParameter( ClassName.get( variable.asType() ), variable.getSimpleName().toString(), Modifier.FINAL );
            if ( isAnnotatedWith( variable, PathParam.class ) ) {
                AnnotationMirror pathParamAnnotation = getAnnotationMirror( variable, PathParam.class );
                AnnotationValue pathParamValue = getAnnotationValue( pathParamAnnotation, "value" );
                initRestBuilder.add( "\n.addPathParam($S, $L)", pathParamValue.getValue(), variable.getSimpleName() );
            } else if ( isAnnotatedWith( variable, QueryParam.class ) ) {
                AnnotationMirror queryParamAnnotation = getAnnotationMirror( variable, QueryParam.class );
                AnnotationValue queryParamValue = getAnnotationValue( queryParamAnnotation, "value" );
                initRestBuilder.add( "\n.addQueryParam($S, $L)", queryParamValue.getValue(), variable.getSimpleName() );
            }
        }

        if ( null != returnTypeReaderGetter ) {
            initRestBuilder.add( "\n.responseConverter($N())", returnTypeReaderGetter );
        }

        initRestBuilder.unindent();

        methodSpecBuilder.addStatement( "return $L", initRestBuilder.build() );

        return methodSpecBuilder.build();
    }

    private AnnotationMirror findHttpMethod( ExecutableElement method ) {
        for ( AnnotationMirror m : method.getAnnotationMirrors() ) {
            if ( m.getAnnotationType().toString().equals( GET.class.getName() ) ) {
                return m;
            }
            if ( m.getAnnotationType().toString().equals( POST.class.getName() ) ) {
                return m;
            }
            if ( m.getAnnotationType().toString().equals( PUT.class.getName() ) ) {
                return m;
            }
            if ( m.getAnnotationType().toString().equals( DELETE.class.getName() ) ) {
                return m;
            }
            if ( m.getAnnotationType().toString().equals( HEAD.class.getName() ) ) {
                return m;
            }
        }
        return null;
    }

    private boolean isAnnotatedWith( Element element, Class<? extends Annotation> clazz ) {
        return element.getAnnotation( clazz ) != null;
    }

    private AnnotationMirror getAnnotationMirror( Element typeElement, Class<?> clazz ) {
        String clazzName = clazz.getName();
        for ( AnnotationMirror m : typeElement.getAnnotationMirrors() ) {
            if ( m.getAnnotationType().toString().equals( clazzName ) ) {
                return m;
            }
        }
        return null;
    }

    private AnnotationValue getAnnotationValue( AnnotationMirror annotationMirror, String key ) {
        for ( Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet() ) {
            if ( entry.getKey().getSimpleName().toString().equals( key ) ) {
                return entry.getValue();
            }
        }
        return null;
    }
}
