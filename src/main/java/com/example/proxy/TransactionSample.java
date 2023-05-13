package com.example.proxy;

import org.springframework.aop.Advisor;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;

public class TransactionSample {

	public static void main(String[] args) {
		TransactionSample transactionSample = new TransactionSample();
		try {
			transactionSample.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void execute() throws Exception {
		@SuppressWarnings("resource")
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:*applicationContext.xml");
		LoginService loginService = (LoginService) applicationContext.getBean("loginService");
		BeanFactoryTransactionAttributeSourceAdvisor factory = (BeanFactoryTransactionAttributeSourceAdvisor)
				applicationContext.getBean("beanFactoryTransactionAttributeSourceAdvisor");
		factory.setAdviceBeanName("transactionInterceptor");
		
		AdvisedSupport advisedSupport = new AdvisedSupport();
		advisedSupport.addAdvisor((Advisor) factory);
		advisedSupport.setTargetSource(new SingletonTargetSource(loginService));
		advisedSupport.setInterfaces(LoginService.class);
		LoginService proxy = (LoginService) advisedSupport.getProxy();
		
		proxy.authenticate();
	}
}
