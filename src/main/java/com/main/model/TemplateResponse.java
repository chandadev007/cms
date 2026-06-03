package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateResponse<T> {
    private String error;
    private String errorDetail;
    private T data;
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }
    public String getErrorDetail() {
        return errorDetail;
    }
    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }

}
