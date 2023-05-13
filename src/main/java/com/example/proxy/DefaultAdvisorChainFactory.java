package com.example.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisorChainFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry;


public class DefaultAdvisorChainFactory implements AdvisorChainFactory {
	
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised advised, Method method, Class<?> targetClass) {
		AdvisorAdapterRegistry registry = new DefaultAdvisorAdapterRegistry();
		Advisor[] advisors = advised.getAdvisors();
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		Boolean hasIntroductions = null;
		
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) {
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				if (advised.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					MethodMatcher methodMatcher = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) methodMatcher).matches(method, actualClass, hasIntroductions);
					}
					else {
						match = methodMatcher.matches(method, actualClass);
					}
					if (match) {
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						if (methodMatcher.isRuntime()) {
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, methodMatcher));
							}
						}
						else {
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (advised.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
		return interceptorList;
	}
	
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static class InterceptorAndDynamicMethodMatcher {
		final MethodInterceptor interceptor;
		final MethodMatcher methodMatcher;
		public InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
			this.interceptor = interceptor;
			this.methodMatcher = methodMatcher;
		}
	}
}
