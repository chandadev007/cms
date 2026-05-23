package com.main.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserData {
    private UserInfo userInfo;
    private List<UserInChargeUnit> listUnitInCharge;
    private List<UserInchargeApp> applications;

    // Getters and Setters
    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public List<UserInchargeApp> getApplications() {
        return applications;
    }

    public void setApplications(List<UserInchargeApp> applications) {
        this.applications = applications;
    }

    public List<UserInChargeUnit> getListUnitInCharge() {
        return listUnitInCharge;
    }

    public void setListUnitInCharge(List<UserInChargeUnit> listUnitInCharge) {
        this.listUnitInCharge = listUnitInCharge;
    }

}