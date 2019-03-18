package com.vic.mynetty.common.future;

import com.vic.mynetty.common.strategyenum.FutureEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NoArgsConstructor
public class FutureMapListener<T> {
//	@Setter
//    @Getter
//	private Executor executor;
	@Getter
	private Runnable runnable;
	@Getter
	private Map<String,FutureListener<T>> listeners;
	
//	public Future(Executor executor) {
//		this.executor = executor;
//	}
	
	public FutureMapListener<T> setRunnable(Runnable runnable) {
		this.runnable = runnable;
		return this;
	}
	
	public FutureMapListener<T> addListener(String key,FutureListener<T> listener) {
		if (listeners == null) {
			listeners = new ConcurrentHashMap<String, FutureListener<T>>();
		}
		listeners.put(key,listener);
		return this;
	}
	public void begin() {
		runnable.run();
	}
	public void fireEvent(FutureEvent event, T t, Exception e) {
		if (listeners != null) {
			for (FutureListener<T> listener : listeners.values()) {
				listener.onEvent(event, t, e);
			}
		}
	}
}
