package com.example.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.ClassUtils;

public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {
	
	private PlatformTransactionManager transactionManager;
	private int timeout = 1000;
	
	public TransactionInterceptor(PlatformTransactionManager transactionManager) {
		this(transactionManager, 1000);
	}
	
	public TransactionInterceptor(PlatformTransactionManager transactionManager, int timeout) {
		this.transactionManager = transactionManager;
		this.timeout = timeout;
	}
	
	public Object invoke(Method method, Object target, Object... args) {
		Class<?> targetClass = target.getClass();
		final TransactionAttribute transactionAttribute = getTransactionAttribute();
		final String joinpointIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
		TransactionInfo transactionInfo = createTransactionIfNecessary(transactionManager, transactionAttribute,
				joinpointIdentification);
		Object retVal = null;
		try {
			retVal = method.invoke(target, args);
		}
		catch (Throwable ex) {
			completeTransactionAfterThrowing(transactionInfo, ex);
			throw new RuntimeException(ex);
		}
		finally {
			cleanupTransactionInfo(transactionInfo);
		}
		commitTransactionAfterReturning(transactionInfo);
		return retVal;
	}
	
	private TransactionAttribute getTransactionAttribute() {
		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
		Propagation propagation = Propagation.REQUIRED;
		rbta.setPropagationBehavior(propagation.value());
		Isolation isolation = Isolation.DEFAULT;
		rbta.setIsolationLevel(isolation.value());
		rbta.setTimeout(timeout);
		rbta.setReadOnly(false);
		return rbta;
	}
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
		return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
	}
}