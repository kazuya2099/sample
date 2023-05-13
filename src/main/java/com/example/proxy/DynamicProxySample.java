package com.example.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DynamicProxySample {

	public static void main(String[] args) {
		DynamicProxySample sample = new DynamicProxySample();
		try {
			LoginService proxy = (LoginService) sample.createProxy();
			proxy.authenticate();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private Object createProxy() throws ClassNotFoundException {
		LoggingInterceptor loggingInterceptor = new LoggingInterceptor(new JdbcLoginService());
		return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {LoginService.class},
				loggingInterceptor);
	}
	
	private static class LoggingInterceptor implements InvocationHandler {
		private Object target;
		
		public LoggingInterceptor(Object target) {
			this.target = target;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			System.out.println("DynamicProxy:before");
			Object result = method.invoke(target, args);
			System.out.println("DynamicProxy:before");
			return result;
		}
	}
}
