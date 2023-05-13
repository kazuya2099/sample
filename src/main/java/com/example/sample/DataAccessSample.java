package com.example.sample;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.example.dao.EmployeeDao;
import com.example.entity.Employee;

public class DataAccessSample {

	private ClassPathXmlApplicationContext context;

	public static void main(String[] args) {
		DataAccessSample dataAccessSample = new DataAccessSample();
		dataAccessSample.init();
		dataAccessSample.selectEmployee();
	}
	
	private void init() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		this.context = context;
	}
	
	private void selectEmployee() {
		EmployeeDao employeeDao = (EmployeeDao) this.context.getBean("employeeDao");
		Employee employee = employeeDao.selectEmployeeById("0000001");
		System.out.println(employee.getEmployeeCode());
	}
}
