package com.vic.mynetty.netty_client;

import com.vic.mynetty.common.declarative.Failure;
import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Mode;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.decorators.Retriable;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.strategyenum.BackOffStrategy;
import lombok.Data;

import java.lang.reflect.Type;

@Data
public class Communication implements Retriable {
	private CommunicatorClient communicator;

	public Communication(CommunicatorClient communication) {
		this.communicator = communication;
	}

	private Mapping.Model mapping;
	/**
	 * Retriable
	 */
	private Retry.Model retry;
	/**
	 * Schedulable
	 */
//	private Schedule.Model schedule;
	/**
	 * Communication
	 */
	private Object[] parameters;
	private Type returnType;

	public <O> O request(Object[] parameters) throws Exception {
		this.parameters = parameters;
		return communicator.request(this);
	}

//	public <O> Future<O> receive(String unicastId) throws Exception {
//		this.parameters = new Object[]{unicastId};
//		return communicator.receive(this);
//	}

	public void send(Object[] parameters) throws Exception {
		this.parameters = parameters;
		communicator.send(this);
	}
	
	/**
	 * Builder: convenient class to create communication. 
	 */
	public static class Builder {
		private Communication model;
		public Builder(CommunicatorClient communicator) {
			this.model = new Communication(communicator);
		}
		public Builder setMapping(Mapping.Model mapping) {
			this.model.setMapping(mapping);
			return this;
		}
		public Builder setPath(String path) {
			Mapping.Model mapping = this.model.getMapping();
			if (mapping == null) {
				mapping = new Mapping.Model();
				this.model.setMapping(mapping);
			}
			mapping.setPath(new Path(path));
			return this;
		}
		public Builder setMode(Mode mode) {
			Mapping.Model mapping = this.model.getMapping();
			if (mapping == null) {
				mapping = new Mapping.Model();
				this.model.setMapping(mapping);
			}
			mapping.setMode(mode);
			return this;
		}
		public Builder setType(com.vic.mynetty.common.declarative.Type type) {
			Mapping.Model mapping = this.model.getMapping();
			if (mapping == null) {
				mapping = new Mapping.Model();
				this.model.setMapping(mapping);
			}
			mapping.setType(type);
			return this;
		}
		public Builder setRetry(Retry.Model retry) {
			this.model.setRetry(retry);
			return this;
		}
		public Builder setTimes(long times) {
			Retry.Model retry = this.model.getRetry();
			if (retry == null) {
				retry = new Retry.Model();
				this.model.setRetry(retry);
			}
			retry.setTimes(times);
			return this;
		}
		public Builder setFailures(Failure[] failures) {
			Retry.Model retry = this.model.getRetry();
			if (retry == null) {
				retry = new Retry.Model();
				this.model.setRetry(retry);
			}
			retry.setFailures(failures);
			return this;
		}
		public Builder setExceptions(Class<? extends Exception>[] exceptions) {
			Retry.Model retry = this.model.getRetry();
			if (retry == null) {
				retry = new Retry.Model();
				this.model.setRetry(retry);
			}
			retry.setExceptions(exceptions);
			return this;
		}
		public Builder setBackOffStrategy(BackOffStrategy retryStrategy) {
			Retry.Model retry = this.model.getRetry();
			if (retry == null) {
				retry = new Retry.Model();
				this.model.setRetry(retry);
			}
			retry.setBackoffStrategy(retryStrategy);
			return this;
		}
		public Builder setRetryParameters(double[] retryParameters) {
			Retry.Model retry = this.model.getRetry();
			if (retry == null) {
				retry = new Retry.Model();
				this.model.setRetry(retry);
			}
			retry.setRetryParameters(retryParameters);
			return this;
		}
//		public Builder setSchedule(Schedule.Model schedule) {
//			this.model.setSchedule(schedule);
//			return this;
//		}
//		public Builder setDelay(long delay) {
//			Schedule.Model schedule = this.model.getSchedule();
//			if (schedule == null) {
//				schedule = new Schedule.Model();
//			}
//			schedule.setDelay(delay);
//			return this;
//		}
//		public Builder setPeriod(long period) {
//			Schedule.Model schedule = this.model.getSchedule();
//			if (schedule == null) {
//				schedule = new Schedule.Model();
//			}
//			schedule.setPeriod(period);
//			return this;
//		}
		public Builder setReturnType(Type returnType) {
			this.model.setReturnType(returnType);
			return this;
		}
		public Communication build() {
			return this.model;
		}
	}
}
