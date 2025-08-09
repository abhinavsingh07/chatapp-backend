package com.chatapp.synk.response;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ErrorResponse<T> {
    private int responseCode;
    private HttpStatus error;
    private String errorMessage;

    public ErrorResponse(int responseCode, HttpStatus error, String errorMessage) {
        this.responseCode = responseCode;
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public ErrorResponse() {
        // Default constructor for deserialization
    }

    private List<T> errors;

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<T> getErrors() {
        return errors;
    }

    public void setErrors(List<T> errors) {
        this.errors = errors;
    }

    public HttpStatus getError() {
        return error;
    }

    public void setError(HttpStatus error) {
        this.error = error;
    }
}
