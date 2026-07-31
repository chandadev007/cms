package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportFull {

  private String customerNameEn;
  private String dateAssessment;
  private String currentAssessment;
  private String currentScore;
  private String status;
  private String gender;
  private String birthDate;
  private String nationalId;
  private String customerNameKh;
  private String legalDocName;
  private String nationality;;
  private String residence;;
  private String sector;
  private String branchCode;
  private String issuedDate;
  private String expiredDate;

  private String channel;
  private String requestType;
  
  public String getCustomerNameEn() {
    return customerNameEn;
  }
  public void setCustomerNameEn(String customerNameEn) {
    this.customerNameEn = customerNameEn;
  }
  public String getDateAssessment() {
    return dateAssessment;
  }
  public void setDateAssessment(String dateAssessment) {
    this.dateAssessment = dateAssessment;
  }
  public String getCurrentAssessment() {
    return currentAssessment;
  }
  public void setCurrentAssessment(String currentAssessment) {
    this.currentAssessment = currentAssessment;
  }
  public String getCurrentScore() {
    return currentScore;
  }
  public void setCurrentScore(String currentScore) {
    this.currentScore = currentScore;
  }
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public String getGender() {
    return gender;
  }
  public void setGender(String gender) {
    this.gender = gender;
  }
  public String getBirthDate() {
    return birthDate;
  }
  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }
  public String getNationalId() {
    return nationalId;
  }
  public void setNationalId(String nationalId) {
    this.nationalId = nationalId;
  }
  public String getCustomerNameKh() {
    return customerNameKh;
  }
  public void setCustomerNameKh(String customerNameKh) {
    this.customerNameKh = customerNameKh;
  }
  public String getLegalDocName() {
    return legalDocName;
  }
  public void setLegalDocName(String legalDocName) {
    this.legalDocName = legalDocName;
  }
  public String getNationality() {
    return nationality;
  }
  public void setNationality(String nationality) {
    this.nationality = nationality;
  }
  public String getResidence() {
    return residence;
  }
  public void setResidence(String residence) {
    this.residence = residence;
  }
  public String getSector() {
    return sector;
  }
  public void setSector(String sector) {
    this.sector = sector;
  }
  public String getBranchCode() {
    return branchCode;
  }
  public void setBranchCode(String branchCode) {
    this.branchCode = branchCode;
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
  public String getChannel() {
    return channel;
  }
  public void setChannel(String channel) {
    this.channel = channel;
  }
  public String getRequestType() {
    return requestType;
  }
  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }
  
}
