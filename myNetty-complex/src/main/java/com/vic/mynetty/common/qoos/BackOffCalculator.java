package com.vic.mynetty.common.qoos;

import com.vic.mynetty.common.strategyenum.BackOffStrategy;
import com.vic.mynetty.utils.PrintUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomUtils;

/**
 *   BackOffStrategy reconnectStrategy = BackOffStrategy.RANDOM_BACKOFF;
 */
public class BackOffCalculator {
	/*
	 * 
	 */
	private BackOffStrategy backOffStrategy;
	private double[] parameters;
	/*
	 * 
	 */
	@Setter
    @Getter
	private long triedTimes;
	/*
	 * 
	 */
	private TrafficRegulator trafficRegulator;
	
	public BackOffCalculator(
			BackOffStrategy backOffStrategy,
			double[] parameters,
			TrafficRegulator trafficRegulator) {
		this.backOffStrategy = backOffStrategy;
		this.parameters = parameters;
		this.trafficRegulator = trafficRegulator;
	}
	
	public long calculate() {
		long backOffTime = 0;
		// no delay on first retry
		if (triedTimes ++ == 0) {
			return 0L;
		}
		// calculate based on different backOffStrategy and backOffParameters
		switch (backOffStrategy) {
		case IMMEDIATE:
			return 0L;
		case RANDOM_BACKOFF:
			int start = (int) parameters[0];
			int limit;
			if (parameters.length == 1) {
				start = 0;
				limit = (int) parameters[0];
			} else if (parameters.length == 2) {
				start = (int) parameters[0];
				limit = (int) parameters[1];
			} else {
				throw new IllegalArgumentException(String.format("wrong parameters 4 RANDOM_BACKOFF|current=%s|expected=%s", 
						PrintUtil.print(parameters), "[{limit}] or [{start},{limit}]"));
			}
			backOffTime = (long) RandomUtils.nextDouble(start, limit);
			break;
		case EXPONENT_BACKOFF:
			if (parameters.length != 3) {
				throw new IllegalArgumentException(String.format("wrong parameters 4 EXPONENT_BACKOFF|current=%s|expected=%s", 
						PrintUtil.print(parameters), "[{base},{exp},{max}]"));
			}
			double base = parameters[0];
			double exp = parameters[1];
			backOffTime = (long) (base * Math.pow(exp, triedTimes));
			if (parameters.length == 3 && backOffTime > parameters[2]) {
				backOffTime = (long) parameters[2];
			}
			break;
		case CONSTANT_BACKOFF:
			if (parameters.length != 1) {
				throw new IllegalArgumentException(String.format("wrong parameters 4 CONSTANT_BACKOFF|current=%s|expected=%s", 
						PrintUtil.print(parameters), "[{constance}]"));
			}
			backOffTime = (long) parameters[0];
			break;
		default:
			break;
		}
		return trafficRegulator.regulate(backOffTime);
	}
	
	public void reset() {
		triedTimes = 0;
	}
}
