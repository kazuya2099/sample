package com.example.proxy;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

public class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {
	
	private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");
	
	private final AdvisedSupport advised;
	private TargetSource targetSource;

	public JdkDynamicAopProxy(AdvisedSupport advisedSupport) throws AopConfigException {
		if (advisedSupport.getAdvisors().length == 0 && advisedSupport.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = advisedSupport;
	}
	
	public void setTargetSource(TargetSource targetSource) {
		this.targetSource = targetSource;
	}

	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(@Nullable ClassLoader classLoader) {
		Class<?>[] proxiedInterfaces = completeProxiedInterfaces(this.advised, true);
		return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
	}
	
	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
		if (specifiedInterfaces.length == 0) {
			Class<?> targetClass = advised.getTargetClass();
			if (targetClass != null) {
				if (targetClass.isInterface()) {
					advised.setInterfaces(targetClass);
				}
				else if (Proxy.isProxyClass(targetClass)) {
					advised.setInterfaces(targetClass.getInterfaces());
				}
				specifiedInterfaces = advised.getProxiedInterfaces();
			}
		}
		boolean addSpringProxy = !advised.isInterfaceProxied(SpringProxy.class);
		boolean addAdvised = !advised.isOpaque() && !advised.isInterfaceProxied(Advised.class);
		boolean addDecoratingProxy = (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class));
		int nonUserIfcCount = 0;
		if (addSpringProxy) {
			nonUserIfcCount++;
		}
		if (addAdvised) {
			nonUserIfcCount++;
		}
		if (addDecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] proxiedInterfaces = new Class<?>[specifiedInterfaces.length + nonUserIfcCount];
		System.arraycopy(specifiedInterfaces, 0, proxiedInterfaces, 0, specifiedInterfaces.length);
		int index = specifiedInterfaces.length;
		if (addSpringProxy) {
			proxiedInterfaces[index] = SpringProxy.class;
			index++;
		}
		if (addAdvised) {
			proxiedInterfaces[index] = Advised.class;
			index++;
		}
		if (addDecoratingProxy) {
			proxiedInterfaces[index] = DecoratingProxy.class;
		}
		return proxiedInterfaces;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("DynamicProxy start");
		
		Object target = targetSource.getTarget();
		Class<?> targetClass = (target != null ? target.getClass() : null);
		List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
		
		Object retVal = null;
		if (chain.isEmpty()) {
			Object[] argsToUse = adaptArgumentsIfNecessary(method, args);
			retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
		}
		else {
			MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
			retVal = invocation.proceed();
		}
		
		Class<?> returnType = method.getReturnType();
		if (retVal != null && retVal == target && returnType != Object.class && returnType.isInstance(proxy) &&
				!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
			retVal = proxy;
		}
		else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
			throw new AopInvocationException("Null return value from advice does not match primitive : " + method);
		}
		
		System.out.println("DynamicProxy end");
		return retVal;
	}

	private static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
		if (ObjectUtils.isEmpty(arguments)) {
			return new Object[0];
		}
		if (method.isVarArgs()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			if (paramTypes.length == arguments.length) {
				int varargIndex = paramTypes.length - 1;
				Class<?> varargType = paramTypes[varargIndex];
				if (varargType.isArray()) {
					Object varargArray = arguments[varargIndex];
					if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
						Object[] newArguments = new Object[arguments.length];
						System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
						Class<?> targetElementType = varargType.getComponentType();
						int varargLength = Array.getLength(varargArray);
						Object newVarargArray = Array.newInstance(targetElementType, varargLength);
						System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
						newArguments[varargIndex] = newVarargArray;
						return newArguments;
					}
				}
			}
		}
		return arguments;
	}
	
	private static Object setCurrentProxy(Object proxy) {
		Object old = currentProxy.get();
		if (proxy != null) {
			currentProxy.set(proxy);
		}
		else {
			currentProxy.remove();
		}
		return old;
	}

	private static class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {
		private final Object proxy;
		private final Object target;
		private final Method method;
		private Object[] arguments = new Object[0];
		private final Class<?> targetClass;
		private final List<?> interceptorsAndDynamicMethodMatchers;
		private int currentInterceptorIndex = -1;
		private Map<String, Object> userAttributes;
		
		public ReflectiveMethodInvocation(Object proxy, Object target, Method method, Object[] arguments,
			Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {
			this.proxy = proxy;
			this.target = target;
			this.targetClass = targetClass;
			this.method = BridgeMethodResolver.findBridgedMethod(method);
			this.arguments = adaptArgumentsIfNecessary(method, arguments);
			this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
		}
		
		@Override
		public Object proceed() throws Throwable {
			if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
				return invokeJoinpoint();
			}
			Object interceptorOrInterceptionAdvice = this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
			if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
				InterceptorAndDynamicMethodMatcher dm = (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
				Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
				if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
					return dm.interceptor.invoke(this);
				}
				else {
					return proceed();
				}
			}
			else {
				return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
			}
		}
	
		protected Object invokeJoinpoint() throws Throwable {
			return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
		}

		@Override
		public Method getMethod() {
			return this.method;
		}

		@Override
		public Object[] getArguments() {
			return this.arguments;
		}

		@Override
		public Object getThis() {
			return this.target;
		}

		@Override
		public AccessibleObject getStaticPart() {
			return this.method;
		}

		@Override
		public Object getProxy() {
			return this.proxy;
		}

		@Override
		public MethodInvocation invocableClone() {
			Object[] cloneArguments = this.arguments;
			if (this.arguments.length > 0) {
				cloneArguments = new Object[this.arguments.length];
				System.arraycopy(this.arguments, 0, cloneArguments, 0, this.arguments.length);
			}
			return invocableClone(cloneArguments);
		}

		@Override
		public MethodInvocation invocableClone(Object... arguments) {
			if (this.userAttributes == null) {
				this.userAttributes = new HashMap<>();
			}
			try {
				ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
				clone.arguments = arguments;
				return clone;
			}
			catch (CloneNotSupportedException ex) {
				throw new IllegalStateException("Should be able to clone object of type [" + getClass() + "]: " + ex);
			}
		}

		@Override
		public void setArguments(Object... arguments) {
			this.arguments = arguments;
		}

		@Override
		public void setUserAttribute(String key, Object value) {
			if (value != null) {
				if (this.userAttributes == null) {
					this.userAttributes = new HashMap<>();
				}
				this.userAttributes.put(key, value);
			}
			else {
				if (this.userAttributes != null) {
					this.userAttributes.remove(key);
				}
			}
		}

		@Override
		public Object getUserAttribute(String key) {
			return (this.userAttributes != null ? this.userAttributes.get(key) : null);
		}
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
