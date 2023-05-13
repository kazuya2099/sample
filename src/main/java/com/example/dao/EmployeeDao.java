package com.example.dao;

import com.example.entity.Employee;

public interface EmployeeDao {

	Employee selectEmployeeById(String employeeCode);
}
