package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ekyc {

    private String id;
    private String appCode;
    private String appChannel;

    private String idNumber;
    private String firstNameKh;
    private String lastNameKh;
    private String firstNameEn;
    private String lastNameEn;
    private String dob;
    private String issuedDate;
    private String expiredDate;
    private String gender;

    private String score;
    private String faceScore;
    private String type;
    private String note;
    private String selfiePath;
    private String status;
    private String statusDesc;
    private String errorDetail;

    private HistoryAction step1;
    private HistoryAction step2;
    private HistoryAction step3;

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

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
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

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(String issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(String expiredDate) {
        this.expiredDate = expiredDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSelfiePath() {
        return selfiePath;
    }

    public void setSelfiePath(String selfiePath) {
        this.selfiePath = selfiePath;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

	public HistoryAction getStep1() {
		return step1;
	}

	public void setStep1(HistoryAction step1) {
		this.step1 = step1;
	}

	public HistoryAction getStep2() {
		return step2;
	}

	public void setStep2(HistoryAction step2) {
		this.step2 = step2;
	}

	public HistoryAction getStep3() {
		return step3;
	}

	public void setStep3(HistoryAction step3) {
		this.step3 = step3;
	}

    public String getFaceScore() {
        return faceScore;
    }

    public void setFaceScore(String faceScore) {
        this.faceScore = faceScore;
    }

}
