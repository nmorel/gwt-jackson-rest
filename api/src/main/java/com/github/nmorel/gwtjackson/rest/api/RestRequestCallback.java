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

import com.github.nmorel.gwtjackson.client.ObjectReader;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

/**
 * @author Nicolas Morel
 */
class RestRequestCallback<R> implements RequestCallback {

    private final ObjectReader<R> responseConverter;

    private final RestCallback<R> successCallback;

    private final ErrorCallback errorCallback;

    RestRequestCallback( ObjectReader<R> responseConverter, RestCallback<R> successCallback, ErrorCallback errorCallback ) {
        this.responseConverter = responseConverter;
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
    }

    @Override
    public void onResponseReceived( Request request, Response response ) {
        // TODO verify status code
        if ( null != successCallback ) {
            R result;
            if ( null != responseConverter ) {
                result = responseConverter.read( response.getText() );
            } else {
                result = null;
            }
            successCallback.onSuccess( response, result );
        }
    }

    @Override
    public void onError( Request request, Throwable exception ) {
        if ( null == errorCallback ) {
            throw new RuntimeException( exception );
        } else {
            errorCallback.onError( null, exception );
        }
    }
}
