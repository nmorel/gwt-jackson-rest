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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.nmorel.gwtjackson.client.ObjectMapper;
import com.github.nmorel.gwtjackson.client.ObjectReader;
import com.github.nmorel.gwtjackson.client.ObjectWriter;
import com.github.nmorel.gwtjackson.rest.api.RestCallback;
import com.github.nmorel.gwtjackson.rest.api.RestRequestBuilder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Processor handling type annotated with {@link GenRestBuilder}.
 */
public class GenRestBuilderProcessor extends AbstractProcessor {

    private Filer filer;

    private Messager messager;

    private Options options;

    @Override
    public Set<String> getSupportedOptions() {
        return Options.getOptionsName();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<String>();
        annotations.add( GenRestBuilder.class.getCanonicalName() );
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init( ProcessingEnvironment processingEnv ) {
        super.init( processingEnv );

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();

        options = new Options( processingEnv.getOptions() );
    }

    @Override
    public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        for ( Element element : roundEnv.getElementsAnnotatedWith( GenRestBuilder.class ) ) {

            if ( !isAnnotatedWith( element, Path.class ) ) {
                error( element, "Only classes and interfaces annotated with @%s are supported", Path.class.getCanonicalName() );
                continue;
            }

            RestService service = new RestService( options, element );

            // For each methods in error, we log the message
            if ( !service.getMethodsInError().isEmpty() ) {
                for ( Entry<ExecutableElement, Exception> entry : service.getMethodsInError().entrySet() ) {
                    try {
                        throw entry.getValue();
                    } catch ( MoreThanOneBodyParamException e ) {
                        warn( entry.getKey(), "Cannot have more than one body parameter" );
                    } catch ( MissingGenResponseClassTypeException e ) {
                        note( entry.getKey(),
                                "Methods with return type javax.ws.rs.core.Response can be annotated with @%s to define an other type.",
                                GenResponseClassType.class.getCanonicalName() );
                    } catch ( Exception e ) {
                        error( entry.getKey(), "Unexpected error: " + e.getMessage() );
                    }
                }
            }

            TypeSpec type = generateBuilder( service );

            try {
                JavaFileObject jfo = filer.createSourceFile( service.getBuilderQualifiedClassName() );
                JavaFile file = JavaFile.builder( service.getPackageName(), type ).build();
                Writer writer = jfo.openWriter();
                file.writeTo( writer );
                writer.close();
            } catch ( IOException e ) {
                error( null, e.getMessage() );
                return true; // Exit processing
            }
        }
        return true;
    }

    /**
     * Generate the rest service builder
     *
     * @param restService The rest service
     */
    private TypeSpec generateBuilder( RestService restService ) {

        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder( restService.getBuilderSimpleClassName() )
                .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                .addJavadoc( "Generated REST service builder for {@link $L}.\n", restService.getTypeElement().getQualifiedName() )
                .addMethod( MethodSpec.constructorBuilder().addModifiers( Modifier.PRIVATE ).build() );

        Map<TypeMirror, MethodSpec> mapperGetters = buildMappers( typeBuilder, restService );

        for ( RestServiceMethod method : restService.getMethods() ) {
            buildMethod( typeBuilder, mapperGetters, method );
        }

        return typeBuilder.build();
    }

    private Map<TypeMirror, MethodSpec> buildMappers( TypeSpec.Builder typeBuilder, RestService restService ) {
        Set<TypeMirror> readers = new LinkedHashSet<TypeMirror>( restService.getReturnTypes() );
        readers.removeAll( restService.getBodyTypes() );

        Set<TypeMirror> writers = new LinkedHashSet<TypeMirror>( restService.getBodyTypes() );
        writers.removeAll( restService.getReturnTypes() );

        Set<TypeMirror> mappers = new LinkedHashSet<TypeMirror>( restService.getBodyTypes() );
        mappers.retainAll( restService.getReturnTypes() );

        Map<TypeMirror, MethodSpec> result = new LinkedHashMap<TypeMirror, MethodSpec>();
        result.putAll( buildMappers( restService.getPackageName(), restService
                .getBuilderSimpleClassName(), typeBuilder, readers, ObjectReader.class ) );
        result.putAll( buildMappers( restService.getPackageName(), restService
                .getBuilderSimpleClassName(), typeBuilder, writers, ObjectWriter.class ) );
        result.putAll( buildMappers( restService.getPackageName(), restService
                .getBuilderSimpleClassName(), typeBuilder, mappers, ObjectMapper.class ) );
        return result;
    }

    private Map<TypeMirror, MethodSpec> buildMappers( String packageName, String className, TypeSpec.Builder typeBuilder, Set<TypeMirror>
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

    private void buildMethod( TypeSpec.Builder typeBuilder, Map<TypeMirror, MethodSpec> mapperGetters, RestServiceMethod method ) {
        String methodName = method.getMethod().getSimpleName().toString();

        AnnotationMirror httpMethodAnnotation = method.getHttpMethodAnnotation();

        TypeMirror returnType = method.getReturnType();
        MethodSpec returnTypeReaderGetter = mapperGetters.get( returnType );
        TypeName returnTypeName;
        if ( null == returnTypeReaderGetter ) {
            returnTypeName = ClassName.get( Void.class );
        } else {
            returnTypeName = TypeName.get( returnType );
        }

        VariableElement bodyVariable = method.getBodyParamVariable();
        MethodSpec bodyTypeWriterGetter;
        TypeName bodyTypeName;

        if ( null != bodyVariable ) {
            bodyTypeWriterGetter = mapperGetters.get( bodyVariable.asType() );
            bodyTypeName = TypeName.get( bodyVariable.asType() );
        } else {
            bodyTypeWriterGetter = null;
            bodyTypeName = ClassName.get( Void.class );
        }

        TypeName restType = ParameterizedTypeName.get( ClassName.get( RestRequestBuilder.class ), bodyTypeName, returnTypeName );

        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder( methodName )
                .addModifiers( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                .returns( restType );

        MethodSpec.Builder methodWithCallbackSpecBuilder = MethodSpec.methodBuilder( methodName )
                .addModifiers( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                .returns( Request.class );

        CodeBlock.Builder initRestBuilder = CodeBlock.builder()
                .add( "new $T()", restType )
                .indent()
                .add( "\n.method($T.$L)", RequestBuilder.class, httpMethodAnnotation.getAnnotationType().asElement().getSimpleName() )
                .add( "\n.url($S)", method.getUrl() );

        if ( null != bodyVariable ) {
            initRestBuilder.add( "\n.body($L)", bodyVariable.getSimpleName() );
        }
        if ( null != bodyTypeWriterGetter ) {
            initRestBuilder.add( "\n.bodyConverter($N())", bodyTypeWriterGetter );
        }
        if ( null != method.getConsumes() ) {
            initRestBuilder.add( "\n.addHeader($S, $S)", "Content-Type", method.getConsumes() );
        }
        if ( null != method.getProduces() ) {
            initRestBuilder.add( "\n.addHeader($S, $S)", "Accept", method.getProduces() );
        }

        StringBuilder callParamBuilder = new StringBuilder();

        for ( VariableElement variable : method.getMethod().getParameters() ) {
            if ( isAnnotatedWith( variable, Context.class ) ) {
                continue;
            }

            ParameterSpec parameterSpec = ParameterSpec.builder( ClassName.get( variable.asType() ), variable.getSimpleName()
                    .toString(), Modifier.FINAL ).build();
            methodSpecBuilder.addParameter( parameterSpec );
            methodWithCallbackSpecBuilder.addParameter( parameterSpec );

            if ( callParamBuilder.length() > 0 ) {
                callParamBuilder.append( ", " );
            }
            callParamBuilder.append( variable.getSimpleName().toString() );

            if ( isAnnotatedWith( variable, PathParam.class ) ) {
                PathParam pathParamAnnotation = variable.getAnnotation( PathParam.class );
                initRestBuilder.add( "\n.addPathParam($S, $L)", pathParamAnnotation.value(), variable.getSimpleName() );
            } else if ( isAnnotatedWith( variable, QueryParam.class ) ) {
                QueryParam queryParamAnnotation = variable.getAnnotation( QueryParam.class );
                initRestBuilder.add( "\n.addQueryParam($S, $L)", queryParamAnnotation.value(), variable.getSimpleName() );
            }
        }

        if ( null != returnTypeReaderGetter ) {
            initRestBuilder.add( "\n.responseConverter($N())", returnTypeReaderGetter );
        }

        initRestBuilder.unindent();

        methodSpecBuilder.addStatement( "return $L", initRestBuilder.build() );
        MethodSpec methodSpec = methodSpecBuilder.build();
        typeBuilder.addMethod( methodSpec );

        methodWithCallbackSpecBuilder.addParameter( ParameterizedTypeName
                .get( ClassName.get( RestCallback.class ), returnTypeName ), "_callback_" );
        methodWithCallbackSpecBuilder.addStatement( "return $L", CodeBlock.builder()
                .add( "$L($L)", methodName, callParamBuilder )
                .indent()
                .add( "\n.callback(_callback_)" )
                .add( "\n.send()" )
                .unindent()
                .build() );
        typeBuilder.addMethod( methodWithCallbackSpecBuilder.build() );
    }

    private boolean isAnnotatedWith( Element element, Class<? extends Annotation> clazz ) {
        return element.getAnnotation( clazz ) != null;
    }

    /**
     * Prints a note message
     *
     * @param e The element which has caused the error. Can be null
     * @param msg The error message
     * @param args if the error message contains %s, %d etc. placeholders this arguments will be used
     * to replace them
     */
    public void note( Element e, String msg, Object... args ) {
        messager.printMessage( Diagnostic.Kind.NOTE, String.format( msg, args ), e );
    }

    /**
     * Prints a warning message
     *
     * @param e The element which has caused the error. Can be null
     * @param msg The error message
     * @param args if the error message contains %s, %d etc. placeholders this arguments will be used
     * to replace them
     */
    public void warn( Element e, String msg, Object... args ) {
        messager.printMessage( Diagnostic.Kind.WARNING, String.format( msg, args ), e );
    }

    /**
     * Prints an error message
     *
     * @param e The element which has caused the error. Can be null
     * @param msg The error message
     * @param args if the error message contains %s, %d etc. placeholders this arguments will be used
     * to replace them
     */
    public void error( Element e, String msg, Object... args ) {
        messager.printMessage( Diagnostic.Kind.ERROR, String.format( msg, args ), e );
    }
}
