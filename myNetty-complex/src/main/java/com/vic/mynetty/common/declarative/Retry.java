package com.vic.mynetty.common.declarative;

import com.vic.mynetty.common.strategyenum.BackOffStrategy;
import lombok.Data;

import java.lang.annotation.*;
import java.lang.reflect.Method;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retry {
	long DEFAULT_TIMEOUT_MILLS = 86400000;
	
	long times() default -1;
	Failure[] failures() default {Failure.TIMEOUT};
	Class<?>[] exceptions() default {Exception.class};
	long timeout() default DEFAULT_TIMEOUT_MILLS;
	BackOffStrategy backOffStrategy() default BackOffStrategy.RANDOM_BACKOFF;
	double[] retryParameters() default {0, 1000};
	@Data
	public static class Model {
		private long times;
		private Failure[] failures;
		private Class<?>[] exceptions;
		private long timeout;
		private BackOffStrategy backoffStrategy;
		private double[] retryParameters;
		public boolean contains(Failure failure) {
			for (Failure item : failures) {
				if (item == failure) {
					return true;
				}
			}
			return false;
		}
	}
	public static class Interpreter implements AnnotationInterpreter<Retry.Model> {

		public Retry.Model interpret(Class<?> clz, Method method) {
			long times = -2;
			Failure[] failures = null;
			Class<?>[] exceptions = null;
			long timeout = -2;
			BackOffStrategy backoffStrategy = null;
			double[] retryParameters = null;

			boolean isAnnotated = false;

			Retry clzRetryAnno = clz.getAnnotation(Retry.class);
			if (clzRetryAnno != null) {
				isAnnotated = true;
				times = clzRetryAnno.times();
				failures = clzRetryAnno.failures();
				exceptions = clzRetryAnno.exceptions();
				timeout = clzRetryAnno.timeout();
				backoffStrategy = clzRetryAnno.backOffStrategy();
				retryParameters = clzRetryAnno.retryParameters();
			}

			Retry methodRetryAnno = method.getAnnotation(Retry.class);
			if (methodRetryAnno != null) {
				isAnnotated = true;
				times = methodRetryAnno.times();
				failures = methodRetryAnno.failures();
				exceptions = methodRetryAnno.exceptions();
				timeout = methodRetryAnno.timeout();
				backoffStrategy = methodRetryAnno.backOffStrategy();
				retryParameters = methodRetryAnno.retryParameters();
			}

			Retry.Model retry = null;
			if (isAnnotated) {
				retry = new Retry.Model();
				retry.setTimes(times);
				retry.setFailures(failures);
				retry.setExceptions(exceptions);
				retry.setTimeout(timeout);
				retry.setBackoffStrategy(backoffStrategy);
				retry.setRetryParameters(retryParameters);
			}
			return retry;
		}
	}
}
