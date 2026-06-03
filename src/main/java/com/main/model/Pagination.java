package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Pagination {
    public int totalPages;
    public int totalElements;
    public int pageIndex;
    public int pageSize;
    public int firstPage;
    public int lastPage;

    public int getTotalPages() {
        return totalPages;
    }
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    public int getTotalElements() {
        return totalElements;
    }
    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }
    public int getPageIndex() {
        return pageIndex;
    }
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }
    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    public int getFirstPage() {
        return firstPage;
    }
    public void setFirstPage(int firstPage) {
        this.firstPage = firstPage;
    }
    public int getLastPage() {
        return lastPage;
    }
    public void setLastPage(int lastPage) {
        this.lastPage = lastPage;
    }
    
}
