package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ekyb {
    private String id;
    private String appCode;
    private String appChannel;
    private String singleId;
    private String tin;
    private String companyNameKh;
    private String companyNameEn;
    private JsonNode dirList;
    private String type;
    private String note;
    private String status;
    private String statusDesc;
    private String score;
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
    public String getSingleId() {
        return singleId;
    }
    public void setSingleId(String singleId) {
        this.singleId = singleId;
    }
    public String getTin() {
        return tin;
    }
    public void setTin(String tin) {
        this.tin = tin;
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
    public JsonNode getDirList() {
        return dirList;
    }
    public void setDirList(JsonNode dirList) {
        this.dirList = dirList;
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
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getScore() {
        return score;
    }
    public void setScore(String score) {
        this.score = score;
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
}
