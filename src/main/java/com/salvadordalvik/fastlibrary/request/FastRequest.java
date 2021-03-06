package com.salvadordalvik.fastlibrary.request;

import android.net.Uri;
import android.util.Log;

import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.HashMap;
import java.util.Map;

/**
 * FastLib
 * Created by Matthew Shepard on 11/17/13.
 */
public abstract class FastRequest<T> {
    private static final String ContentType = "Content-Type";

    public interface FastStatusCallback{
        public void onSuccess(FastRequest request);
        public void onFailure(FastRequest request, VolleyError error);
    }

    private int method;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private HashMap<String, String> params = new HashMap<String, String>();
    private String baseUrl;

    private FastStatusCallback externalCallback;
    private Response.Listener<T> successCallback;
    private Response.ErrorListener errorCallback;

    public FastRequest(String baseUrl, Response.Listener<T> success, Response.ErrorListener error) {
        this(baseUrl, Request.Method.GET, success, error);
    }

    public FastRequest(String baseUrl, int method, Response.Listener<T> success, Response.ErrorListener error) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.successCallback = success;
        this.errorCallback = error;
    }

    public void addParam(String key, String value){
        params.put(key, value);
    }

    public void addParam(String key, int value){
        addParam(key, Integer.toString(value));
    }

    public void addParam(String key, long value){
        addParam(key, Long.toString(value));
    }

    public void addParam(String key, float value){
        addParam(key, Float.toString(value));
    }

    public void addParam(String key, double value){
        addParam(key, Double.toString(value));
    }

    public void addHeader(String key, String value){
        headers.put(key, value);
    }

    private String generateUrl(){
        if(method == Request.Method.GET){
            Uri.Builder url = Uri.parse(baseUrl).buildUpon();
            for(Map.Entry<String, String> param : params.entrySet()){
                url.appendQueryParameter(param.getKey(), param.getValue());
            }
            return url.build().toString();
        }
        return baseUrl;
    }

    public abstract T parseResponse(Request<T> request, NetworkResponse response) throws Exception;

    private byte[] requestBody(){
        return null;
    }

    protected String getBodyType(){
        return "application/x-www-form-urlencoded; charset="+getBodyCharset();
    }

    protected String getBodyCharset(){
        return "UTF-8";
    }

    public Request<T> build(FastStatusCallback callback){
        externalCallback = callback;
        return generateRequest(generateUrl(), successCallback, errorCallback);
    }

    private static final RetryPolicy lenientRetryPolicy = new DefaultRetryPolicy(20000, 1, 1);

    protected Request<T> generateRequest(String url, Response.Listener<T> success, Response.ErrorListener error){
        return new FastInternalRequest(url, success, error);
    }

    protected class FastInternalRequest extends Request<T>{
        private Response.Listener<T> successListener;

        public FastInternalRequest(String url, Response.Listener<T> success, Response.ErrorListener error) {
            super(method, url, error);
            this.successListener = success;
            setRetryPolicy(lenientRetryPolicy);
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try{
                T result = parseResponse(this, response);
                return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
            }catch (VolleyError ve){
                ve.printStackTrace();
                return Response.error(ve);
            }catch (Exception e){
                e.printStackTrace();
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
            if(error != null){
                error.printStackTrace();
            }

            if(externalCallback != null){
                externalCallback.onFailure(FastRequest.this, error);
            }

            if (error != null) {
                Log.e(FastRequest.this.getClass().getSimpleName(), "Error: "+error.toString());
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
            return getBodyType();
        }

        @Override
        protected String getParamsEncoding() {
            return getBodyCharset();
        }
    }

    static String parseCharset(Map<String, String> headers, String fallback){
        String contentType = headers.get(ContentType);
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
