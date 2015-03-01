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

package com.github.nmorel.gwtjackson.rest.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.nmorel.gwtjackson.client.ObjectReader;
import com.github.nmorel.gwtjackson.client.ObjectWriter;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.URL;

/**
 * @author Nicolas Morel
 */
public class    RestRequestBuilder<B, R> {

    private static String defaultApplicationPath = "";

    public static void setDefaultApplicationPath( String defaultApplicationPath ) {
        if ( null == defaultApplicationPath ) {
            throw new IllegalArgumentException( "Application path cannot be null" );
        }
        RestRequestBuilder.defaultApplicationPath = defaultApplicationPath;
    }

    /**
     * HTTP method to use when opening a JavaScript XmlHttpRequest object.
     */
    private Method method;

    /**
     * Application path to concatenate before the url
     */
    private String applicationPath = defaultApplicationPath;

    /**
     * URL to use when opening a JavaScript XmlHttpRequest object.
     */
    private String url;

    /**
     * User to use when opening a JavaScript XmlHttpRequest object.
     */
    private String user;

    /**
     * Password to use when opening a JavaScript XmlHttpRequest object.
     */
    private String password;

    /**
     * Whether to include credentials for a Cross Origin Request.
     */
    private Boolean includeCredentials;

    /**
     * Timeout in milliseconds before the request timeouts and fails.
     */
    private Integer timeoutMillis;

    /**
     * Map of header name to value that will be added to the JavaScript
     * XmlHttpRequest object before sending a request.
     */
    private Map<String, String> headers;

    private Map<String, List<Object>> queryParams;

    private Map<String, Object> pathParams;

    private B body;

    private ObjectWriter<B> bodyConverter;

    private ObjectReader<R> responseConverter;

    private RestCallback<R> successCallback;

    private ErrorCallback errorCallback;

    public RestRequestBuilder() {
    }

    public RestRequestBuilder<B, R> method( Method method ) {
        this.method = method;
        return this;
    }

    public RestRequestBuilder<B, R> applicationPath( String applicationPath ) {
        this.applicationPath = applicationPath;
        return this;
    }

    public RestRequestBuilder<B, R> url( String url ) {
        this.url = url;
        return this;
    }

    public RestRequestBuilder<B, R> user( String user ) {
        this.user = user;
        return this;
    }

    public RestRequestBuilder<B, R> password( String password ) {
        this.password = password;
        return this;
    }

    public RestRequestBuilder<B, R> includeCredentials( boolean includeCredentials ) {
        this.includeCredentials = includeCredentials;
        return this;
    }

    public RestRequestBuilder<B, R> timeout( int timeoutMillis ) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public RestRequestBuilder<B, R> addHeader( String name, String value ) {
        if ( null == headers ) {
            headers = new LinkedHashMap<String, String>();
        }
        headers.put( name, value );
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public RestRequestBuilder<B, R> addQueryParam( String name, Object value ) {
        List<Object> allValues = getQueryParams( name );
        allValues.add( value );
        return this;
    }

    public RestRequestBuilder<B, R> addQueryParam( String name, Collection<Object> values ) {
        List<Object> allValues = getQueryParams( name );
        allValues.addAll( values );
        return this;
    }

    public RestRequestBuilder<B, R> addQueryParam( String name, Object[] values ) {
        List<Object> allValues = getQueryParams( name );
        for ( Object value : values ) {
            allValues.add( value );
        }
        return this;
    }

    public RestRequestBuilder<B, R> addQueryParam( String name, Iterable<Object> values ) {
        List<Object> allValues = getQueryParams( name );
        for ( Object value : values ) {
            allValues.add( value );
        }
        return this;
    }

    private List<Object> getQueryParams( String name ) {
        if ( null == queryParams ) {
            queryParams = new LinkedHashMap<String, List<Object>>();
        }
        List<Object> allValues = queryParams.get( name );
        if ( null == allValues ) {
            allValues = new ArrayList<Object>();
            queryParams.put( name, allValues );
        }
        return allValues;
    }

    public Map<String, List<Object>> getQueryParams() {
        return queryParams;
    }

    public RestRequestBuilder<B, R> addPathParam( String name, Object value ) {
        pathParams.put( name, value );
        return this;
    }

    public Map<String, Object> getPathParams() {
        return pathParams;
    }

    public RestRequestBuilder<B, R> body( B body ) {
        this.body = body;
        return this;
    }

    public RestRequestBuilder<B, R> bodyConverter( ObjectWriter<B> bodyConverter ) {
        this.bodyConverter = bodyConverter;
        return this;
    }

    public RestRequestBuilder<B, R> responseConverter( ObjectReader<R> responseConverter ) {
        this.responseConverter = responseConverter;
        return this;
    }

    public RestRequestBuilder<B, R> successCallback( RestCallback<R> successCallback ) {
        this.successCallback = successCallback;
        return this;
    }

    public RestRequestBuilder<B, R> errorCallback( ErrorCallback errorCallback ) {
        this.errorCallback = errorCallback;
        return this;
    }

    public Request send() {
        if ( null == method ) {
            throw new IllegalArgumentException( "The method is required" );
        }
        if ( null == url ) {
            throw new IllegalArgumentException( "The url is required" );
        }

        String urlWithParams = url;
        if ( null != pathParams && !pathParams.isEmpty() ) {
            for ( Entry<String, Object> pathParam : pathParams.entrySet() ) {
                urlWithParams = urlWithParams.replace( "{" + pathParam.getKey() + "}", pathParam.getValue().toString() );
            }
        }

        StringBuilder urlBuilder = new StringBuilder( applicationPath );
        if ( !applicationPath.endsWith( "/" ) && !urlWithParams.startsWith( "/" ) ) {
            urlBuilder.append( '/' );
        }
        urlBuilder.append( urlWithParams );

        if ( null != queryParams && !queryParams.isEmpty() ) {
            boolean first = true;
            for ( Entry<String, List<Object>> params : queryParams.entrySet() ) {
                String name = URL.encodeQueryString( params.getKey() );
                if ( null != params.getValue() && !params.getValue().isEmpty() ) {
                    for ( Object param : params.getValue() ) {
                        if ( first ) {
                            urlBuilder.append( '?' );
                            first = false;
                        } else {
                            urlBuilder.append( '&' );
                        }
                        urlBuilder.append( name );
                        urlBuilder.append( '=' );
                        urlBuilder.append( URL.encodeQueryString( param.toString() ) );
                    }
                }
            }
        }

        RequestBuilder builder = new RequestBuilder( method, urlBuilder.toString() );
        builder.setHeader( "Content-Type", "application/json; charset=utf-8" );

        if ( null != headers && !headers.isEmpty() ) {
            for ( Entry<String, String> header : headers.entrySet() ) {
                builder.setHeader( header.getKey(), header.getValue() );
            }
        }

        if ( null != user ) {
            builder.setUser( user );
        }

        if ( null != password ) {
            builder.setPassword( password );
        }

        if ( null != includeCredentials ) {
            builder.setIncludeCredentials( includeCredentials );
        }

        if ( null != timeoutMillis ) {
            builder.setTimeoutMillis( timeoutMillis );
        }

        if ( null != body ) {
            if ( null != bodyConverter ) {
                builder.setRequestData( bodyConverter.write( body ) );
            } else {
                builder.setRequestData( body.toString() );
            }
        }

        builder.setCallback( new RestRequestCallback<R>( responseConverter, successCallback, errorCallback ) );

        try {
            return builder.send();
        } catch ( RequestException e ) {
            throw new RuntimeException( e );
        }
    }

}