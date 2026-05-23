package com.main.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRole {
    private String roleCode;
    private String roleName;
    private List<UserMenu> menus;

	public String getRoleCode() {
		return roleCode;
	}
	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	public List<UserMenu> getMenus() {
		return menus;
	}
	public void setMenus(List<UserMenu> menus) {
		this.menus = menus;
	}
}
