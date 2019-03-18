package com.vic.mynetty.common.future;

import com.vic.mynetty.common.strategyenum.FutureEvent;

public interface FutureListener<T> {
	void onEvent(FutureEvent event, T t, Exception e);
}
