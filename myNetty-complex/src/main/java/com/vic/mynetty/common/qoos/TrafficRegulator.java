package com.vic.mynetty.common.qoos;

import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.strategyenum.BackOffStrategy;
import com.vic.mynetty.utils.PrintUtil;
import org.apache.commons.lang3.RandomUtils;

/**
 * 监听session ,改变factor 因子大小，
 * todo  要废弃这个类，感觉这个类的作用不是很大
 * BackOffStrategy trafficRegulationStrategy = BackOffStrategy.EXPONENT_BACKOFF;
 */
public class TrafficRegulator extends SessionEventListenerAdapter {
	/*
	 * control strategy setting
	 */
	private BackOffStrategy controlStrategy;
	private double[] controlParameters;
	/*
	 * increment when NET_DELAY
	 * decrement when NET_RECOVER 
	 */
	private int factor;
	
	public TrafficRegulator(
			BackOffStrategy regulationStrategy,
			double[] regulationParameters) {
		this.controlStrategy = regulationStrategy;
		this.controlParameters = regulationParameters;
	}

	public long regulate(long backOffTime) {
		if (factor == 0) {
			return backOffTime;
		}
		switch (controlStrategy) {
		case IMMEDIATE:
			return backOffTime;
		case RANDOM_BACKOFF:
			double start;
			double limit;
			long randMax;
			if (controlParameters.length == 2) {
				start = 0;
				limit = controlParameters[0] * factor;
				randMax = (long) controlParameters[1];
			} else if (controlParameters.length == 3) {
				start = controlParameters[0];
				limit = controlParameters[1] * factor;
				randMax = (long) controlParameters[2];
			} else {
				throw new IllegalArgumentException(String.format("wrong parameters 4 RANDOM_BACKOFF|current=%s|expected=%s", 
						PrintUtil.print(controlParameters), "[{limit},{max}] or [{start},{limit},{max}]"));
			}
			backOffTime += RandomUtils.nextDouble(start, limit);
			if (backOffTime > randMax) {
				backOffTime = randMax;
			}
			break;
		case EXPONENT_BACKOFF:
			if (controlParameters.length != 2) {
				throw new IllegalArgumentException(String.format("wrong parameters 4 EXPONENT_BACKOFF|current=%s|expected=%s", 
						PrintUtil.print(controlParameters), "[{base},{max}]"));
			}
			double base = controlParameters[0];
			long expMax = (long) controlParameters[1];
			backOffTime *= (long) Math.pow(base, factor);
			if (backOffTime > expMax) {
				backOffTime = expMax;
			}
			break;
		case CONSTANT_BACKOFF:
			if (controlParameters.length != 2) {
				throw new IllegalArgumentException(String.format("wrong parameters 4 CONSTANT_BACKOFF|current=%s|expected=%s", 
						PrintUtil.print(controlParameters), "[{constance}, {max}]"));
			}
			backOffTime += (long) ((controlParameters[0]) * factor);
			if (backOffTime > controlParameters[1]) {
				backOffTime = (long) controlParameters[1];
			}
			break;
		default:
			break;
		}
		return backOffTime;
	}
	
	@Override
	public void onSessionReady(Session session) {
		this.factor = 0;
	}

	@Override
	public void onSessionNetDelay(Session session) {
		this.factor ++;
	}

	@Override
	public void onSessionNetRecover(Session session) {
		if (this.factor > 0) {
			this.factor --;
		}
	}

	
	public static void main(String[] args) {
		TrafficRegulator controller = new TrafficRegulator(BackOffStrategy.CONSTANT_BACKOFF, new double[]{1000});
		controller.onSessionNetDelay(null);
		controller.onSessionNetDelay(null);
		System.out.println(controller.regulate(1000));
		System.out.println(new double[]{1,2});
	}
}
