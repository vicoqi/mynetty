package com.vic.mynetty.common.strategyenum;

public enum BackOffStrategy {
	IMMEDIATE,
	RANDOM_BACKOFF, EXPONENT_BACKOFF, CONSTANT_BACKOFF
}
