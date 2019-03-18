package com.vic.mynetty.common.decorators;


import com.vic.mynetty.common.declarative.Retry;

public interface Retriable {
	Retry.Model getRetry();
}
