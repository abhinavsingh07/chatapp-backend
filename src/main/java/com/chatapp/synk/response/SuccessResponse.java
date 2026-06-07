package com.chatapp.synk.response;

import org.springframework.http.HttpStatus;

import java.util.List;

public class SuccessResponse<T> {

    private HttpStatus responseCode;
    private String message;
    private List<T> data;

    public SuccessResponse(HttpStatus responseCode, String message, List<T> data) {
        this.responseCode = responseCode;
        this.message = message;
        this.data = data;
    }

    public SuccessResponse(HttpStatus responseCode, String message) {
        this.responseCode = responseCode;
        this.message = message;
    }

    public HttpStatus getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(HttpStatus responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
