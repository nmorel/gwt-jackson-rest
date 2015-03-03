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

    private final RestCallback<R> callback;

    RestRequestCallback(ObjectReader<R> responseConverter, RestCallback<R> callback) {
        this.responseConverter = responseConverter;
        this.callback = callback;
    }

    @Override
    public void onResponseReceived(Request request, Response response) {
        if (isSuccessStatusCode(response)) {
            if (null != callback) {
                R result;
                if (null != responseConverter) {
                    try {
                        result = responseConverter.read(response.getText());
                    } catch (Exception e) {
                        onError(request, e);
                        return;
                    }
                } else {
                    result = null;
                }
                callback.onSuccess(response, result);
            }
        } else {
            if (null != callback) {
                callback.onError(response);
            } else {
                throw new RestException("An error occured. Status : " + response.getStatusCode());
            }
        }
    }

    private boolean isSuccessStatusCode(Response response) {
        int statusCode = response.getStatusCode();
        return (statusCode >= 200 && statusCode < 300) || statusCode == 304;
    }

    @Override
    public void onError(Request request, Throwable exception) {
        if (null == callback) {
            throw new RestException(exception);
        } else {
            callback.onFailure(exception);
        }
    }
}
