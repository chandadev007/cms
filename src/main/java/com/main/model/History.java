package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class History {
    private String id; 
    private String appCode;
    private String appChannel;
    private String requestType; 
    private String firstNameKh;
    private String lastNameKh;
    private String firstNameEn;
    private String lastNameEn;
    private String companyNameKh;
    private String companyNameEn;
    private String status;
    private String statusDesc;
	private String score;
	private String faceScore;
    private String userId;
    private String userName;
    private String createTime;

	private String ekycType; 
	private String ekybType;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getAppCode() {
		return appCode;
	}
	public void setAppCode(String appCode) {
		this.appCode = appCode;
	}
	public String getAppChannel() {
		return appChannel;
	}
	public void setAppChannel(String appChannel) {
		this.appChannel = appChannel;
	}
	public String getRequestType() {
		return requestType;
	}
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}
	public String getFirstNameKh() {
		return firstNameKh;
	}
	public void setFirstNameKh(String firstNameKh) {
		this.firstNameKh = firstNameKh;
	}
	public String getLastNameKh() {
		return lastNameKh;
	}
	public void setLastNameKh(String lastNameKh) {
		this.lastNameKh = lastNameKh;
	}
	public String getCompanyNameKh() {
		return companyNameKh;
	}
	public void setCompanyNameKh(String companyNameKh) {
		this.companyNameKh = companyNameKh;
	}
	public String getCompanyNameEn() {
		return companyNameEn;
	}
	public void setCompanyNameEn(String companyNameEn) {
		this.companyNameEn = companyNameEn;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatusDesc() {
		return statusDesc;
	}
	public void setStatusDesc(String statusDesc) {
		this.statusDesc = statusDesc;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getCreateTime() {
		return createTime;
	}
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
	public String getFirstNameEn() {
		return firstNameEn;
	}
	public void setFirstNameEn(String firstNameEn) {
		this.firstNameEn = firstNameEn;
	}
	public String getLastNameEn() {
		return lastNameEn;
	}
	public void setLastNameEn(String lastNameEn) {
		this.lastNameEn = lastNameEn;
	}
	public String getScore() {
		return score;
	}
	public void setScore(String score) {
		this.score = score;
	}
	public String getFaceScore() {
		return faceScore;
	}
	public void setFaceScore(String faceScore) {
		this.faceScore = faceScore;
	}
	public String getEkycType() {
		return ekycType;
	}
	public void setEkycType(String ekycType) {
		this.ekycType = ekycType;
	}
	public String getEkybType() {
		return ekybType;
	}
	public void setEkybType(String ekybType) {
		this.ekybType = ekybType;
	}

}
