package com.example.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisorChainFactory;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.util.ClassUtils;

public class AdvisedSupport implements Advised {
	
	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;
	
	private TargetSource targetSource;
	private List<Advisor> advisors = new ArrayList<>();
	private Advisor[] advisorArray = new Advisor[0];
	private List<Class<?>> interfaces = new ArrayList<>();
	private transient Map<MethodCacheKey, List<Object>> methodCache;
	private boolean frozen = false;
	private boolean proxyTargetClass = false;
	private boolean opaque = false;
	private boolean exposeProxy = false;
	private boolean preFiltered = false;
	private AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	public AdvisedSupport() {
		this.methodCache = new ConcurrentHashMap<>(32);
	}
	
	public void setTargetSource(TargetSource targetSource) {
		this.targetSource = targetSource;
	}
	
	public final Advisor[] getAdvisors() {
		return this.advisorArray;
	}
	
	public Object getProxy() {
		JdkDynamicAopProxy proxy = new JdkDynamicAopProxy(this);
		proxy.setTargetSource(targetSource);
		return proxy.getProxy();
	}
	
	@Override
	public Class<?> getTargetClass() {
		return this.targetSource.getTargetClass();
	}
	
	@Override
	public void addAdvisor(Advisor advisor) {
		int pos = this.advisors.size();
		addAdvisor(pos, advisor);
	}

	@Override
	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
		if (advisor instanceof IntroductionAdvisor) {
			validateIntroductionAdvisor((IntroductionAdvisor) advisor);
		}
		addAdvisorInternal(pos, advisor);
	}

	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
		advisor.validateInterfaces();
		Class<?>[] ifcs = advisor.getInterfaces();
		for (Class<?> ifc : ifcs) {
			addInterface(ifc);
		}
	}
	
	public void setInterfaces(Class<?>... interfaces) {
		this.interfaces.clear();
		for (Class<?> ifc : interfaces) {
			addInterface(ifc);
		}
	}
	
	public void addInterface(Class<?> intf) {
		if (!intf.isInterface()) {
			throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
		}
		if (!this.interfaces.contains(intf)) {
			this.interfaces.add(intf);
			adviceChanged();
		}
	}
	
	public boolean isOpaque() {
		return this.opaque;
	}
	
	protected void adviceChanged() {
		this.methodCache.clear();
	}
	
	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (pos > this.advisors.size()) {
			throw new IllegalArgumentException(	"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
		}
		this.advisors.add(pos, advisor);
		updateAdvisorArray();
		adviceChanged();
	}

	protected final void updateAdvisorArray() {
		this.advisorArray = this.advisors.toArray(new Advisor[0]);
	}

	@Override
	public boolean isFrozen() {
		return this.frozen;
	}

	@Override
	public boolean isProxyTargetClass() {
		return this.proxyTargetClass;
	}

	@Override
	public Class<?>[] getProxiedInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public boolean isInterfaceProxied(Class<?> intf) {
		for (Class<?> proxyIntf : this.interfaces) {
			if (intf.isAssignableFrom(proxyIntf)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public TargetSource getTargetSource() {
		return this.targetSource;
	}

	@Override
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}

	@Override
	public boolean isExposeProxy() {
		return this.exposeProxy;
	}

	@Override
	public void setPreFiltered(boolean preFiltered) {
		this.preFiltered = preFiltered;
	}

	@Override
	public boolean isPreFiltered() {
		return this.preFiltered;
	}

	@Override
	public boolean removeAdvisor(Advisor advisor) {
		int index = indexOf(advisor);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public void removeAdvisor(int index) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
		}
		if (index < 0 || index > this.advisors.size() - 1) {
			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
					"This configuration only has " + this.advisors.size() + " advisors.");
		}
		Advisor advisor = this.advisors.get(index);
		if (advisor instanceof IntroductionAdvisor) {
			IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
			for (int j = 0; j < ia.getInterfaces().length; j++) {
				removeInterface(ia.getInterfaces()[j]);
			}
		}
		this.advisors.remove(index);
		updateAdvisorArray();
		adviceChanged();
	}

	public boolean removeInterface(Class<?> intf) {
		return this.interfaces.remove(intf);
	}
	
	@Override
	public int indexOf(Advisor advisor) {
		return this.advisors.indexOf(advisor);
	}

	@Override
	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
		int index = indexOf(a);
		if (index == -1) {
			return false;
		}
		removeAdvisor(index);
		addAdvisor(index, b);
		return true;
	}

	@Override
	public void addAdvice(Advice advice) throws AopConfigException {
		int pos = this.advisors.size();
		addAdvice(pos, advice);
	}
	
	@Override
	public void addAdvice(int pos, Advice advice) throws AopConfigException {
		if (advice instanceof IntroductionInfo) {
			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
		}
		else if (advice instanceof DynamicIntroductionAdvice) {
			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
		}
		else {
			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
		}
	}

	@Override
	public boolean removeAdvice(Advice advice) {
		int index = indexOf(advice);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public int indexOf(Advice advice) {
		for (int i = 0; i < this.advisors.size(); i++) {
			Advisor advisor = this.advisors.get(i);
			if (advisor.getAdvice() == advice) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public String toProxyConfigString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
		sb.append(this.advisors.size()).append(" advisors ");
		sb.append(this.advisors).append("; ");
		sb.append("targetSource [").append(this.targetSource).append("]; ");
		sb.append(super.toString());
		return sb.toString();
	}
	
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
		MethodCacheKey cacheKey = new MethodCacheKey(method);
		List<Object> cached = this.methodCache.get(cacheKey);
		if (cached == null) {
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}
	
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {
		private final Method method;
		private final int hashCode;

		public MethodCacheKey(Method method) {
			this.method = method;
			this.hashCode = method.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof MethodCacheKey && this.method == ((MethodCacheKey) other).method));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.method.getName().compareTo(other.method.getName());
			if (result == 0) {
				result = this.method.toString().compareTo(other.method.toString());
			}
			return result;
		}
	}
}
