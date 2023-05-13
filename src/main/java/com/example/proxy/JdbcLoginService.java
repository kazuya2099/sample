package com.example.proxy;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.dao.LoginDao;
import com.example.entity.Login;

@Component("loginService")
public class JdbcLoginService implements LoginService {

	@Autowired
	private LoginDao loginDao;
	
	@Override
	@Transactional
	public boolean authenticate() {
		Date now = new Date();
		Login login = new Login();
		login.setEmployeeId("0000001");
		login.setLoginFailedCount(0);
		login.setLoginLastDate(now);
		login.setLoginStatus("0");
		login.setUpdateDate(now);
		login.setUpdateUser("0000001");
		loginDao.updateLogin(login);
		throw new RuntimeException();
	}
}
