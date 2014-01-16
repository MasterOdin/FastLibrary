package com.salvadordalvik.fastlibrary.request;

import android.net.Uri;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import org.apache.http.protocol.HTTP;

import java.util.HashMap;
import java.util.Map;

/**
 * FastLib
 * Created by Matthew Shepard on 11/17/13.
 */
public abstract class FastRequest<T> {
    public interface FastStatusCallback{
        public void onSuccess(FastRequest request);
        public void onFailure(FastRequest request, VolleyError error);
    }

    private int method;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private HashMap<String, String> params = new HashMap<String, String>();
    private Uri.Builder url;

    private FastStatusCallback externalCallback;
    private Response.Listener<T> successCallback;
    private Response.ErrorListener errorCallback;

    public FastRequest(String baseUrl, Response.Listener<T> success, Response.ErrorListener error) {
        this(baseUrl, Request.Method.GET, success, error);
    }

    public FastRequest(String baseUrl, int method, Response.Listener<T> success, Response.ErrorListener error) {
        this.method = method;
        url = Uri.parse(baseUrl).buildUpon();
        successCallback = success;
        errorCallback = error;
    }

    public void addParam(String key, String value){
        params.put(key, value);
    }

    public void addHeader(String key, String value){
        headers.put(key, value);
    }

    public String generateUrl(){
        if(method == Request.Method.GET){
            for(Map.Entry<String, String> param : params.entrySet()){
                url.appendQueryParameter(param.getKey(), param.getValue());
            }
        }
        return url.build().toString();
    }

    public abstract T parseResponse(NetworkResponse response) throws Exception;

    protected byte[] requestBody(){
        return null;
    }

    public Request<T> build(FastStatusCallback callback){
        externalCallback = callback;
        return new FastInternalRequest(generateUrl(), successCallback, errorCallback);
    }

    private class FastInternalRequest extends Request<T>{
        private Response.Listener<T> successListener;

        public FastInternalRequest(String url, Response.Listener<T> success, Response.ErrorListener error) {
            super(method, url, error);
            this.successListener = success;
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try{
                T result = parseResponse(response);
                return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
            }catch (Exception e){
                return Response.error(new ParseError(e));
            }
        }

        @Override
        protected void deliverResponse(T response) {
            if(successListener != null){
                successListener.onResponse(response);
            }
            if(externalCallback != null){
                externalCallback.onSuccess(FastRequest.this);
            }
        }

        @Override
        public void deliverError(VolleyError error) {
            super.deliverError(error);
            if(externalCallback != null){
                externalCallback.onFailure(FastRequest.this, error);
            }
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            return params;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            byte[] body = requestBody();
            if(body != null){
                return body;
            }
            return super.getBody();
        }

        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }
    }

    public static String parseCharset(Map<String, String> headers, String fallback){
        String contentType = headers.get(HTTP.CONTENT_TYPE);
        if (contentType != null) {
            String[] params = contentType.split(";");
            for (int i = 1; i < params.length; i++) {
                String[] pair = params[i].trim().split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("charset")) {
                        return pair[1];
                    }
                }
            }
        }
        return fallback;
    }
}
