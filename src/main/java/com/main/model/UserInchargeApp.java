package com.main.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInchargeApp {
	private String appCode;
	private String appName;
	private String iconUrl;
	private String listChannel;
	private String apiMain;
	private String apiDownload;
	private String apiUpload;
	private List<UserRole> roles;
	private List<UserInChargeUnit> listUnitInCharge;


	public String getAppCode() {
		return appCode;
	}

	public void setAppCode(String appCode) {
		this.appCode = appCode;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public String getApiMain() {
		return apiMain;
	}

	public void setApiMain(String apiMain) {
		this.apiMain = apiMain;
	}

	public String getApiDownload() {
		return apiDownload;
	}

	public void setApiDownload(String apiDownload) {
		this.apiDownload = apiDownload;
	}

	public List<UserRole> getRoles() {
		return roles;
	}

	public void setRoles(List<UserRole> roles) {
		this.roles = roles;
	}

	public String getListChannel() {
		return listChannel;
	}

	public void setListChannel(String listChannel) {
		this.listChannel = listChannel;
	}

	public String getApiUpload() {
		return apiUpload;
	}

	public void setApiUpload(String apiUpload) {
		this.apiUpload = apiUpload;
	}

	public List<UserInChargeUnit> getListUnitInCharge() {
		return listUnitInCharge;
	}

	public void setListUnitInCharge(List<UserInChargeUnit> listUnitInCharge) {
		this.listUnitInCharge = listUnitInCharge;
	}

}