package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateResponseWithPagination<T> {
    private String error;
    private String errorDetail;
    private Object summaryStatus;
    private T data;
    public Pagination pageable;

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
    public Pagination getPageable() {
        return pageable;
    }
    public void setPageable(Pagination pageable) {
        this.pageable = pageable;
    }
    public Object getSummaryStatus() {
        return summaryStatus;
    }
    public void setSummaryStatus(Object summaryStatus) {
        this.summaryStatus = summaryStatus;
    }
}
