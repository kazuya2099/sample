package com.example.entity;

import java.util.Date;

import lombok.Data;

@Data
public class Login {

	private String employeeId;
	private Integer loginFailedCount;
	private Date loginLastDate;
	private String loginStatus;
	private Date createDate;
	private String createUser;
	private Date updateDate;
	private String updateUser;
	private Date deleteDate;
	private String deleteUser;
	private String deleteFlag;
}
