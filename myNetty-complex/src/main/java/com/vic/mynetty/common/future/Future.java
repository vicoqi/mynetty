package com.vic.mynetty.common.future;

import com.vic.mynetty.common.strategyenum.FutureEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@NoArgsConstructor
public class Future<T> {
//	@Setter
//    @Getter
//	private Executor executor;
	@Getter
	private Runnable runnable;
	private List<FutureListener<T>> listeners;
	
//	public Future(Executor executor) {
//		this.executor = executor;
//	}
	
	public Future<T> setRunnable(Runnable runnable) {
		this.runnable = runnable;
		return this;
	}
	
	public Future<T> addListener(FutureListener<T> listener) {
		if (listeners == null) {
			listeners = new ArrayList<FutureListener<T>>();
		}
		listeners.add(listener);
		return this;
	}
	public void begin() {
		runnable.run();
	}
	public void fireEvent(FutureEvent event, T t, Exception e) {
		if (listeners != null) {
			for (FutureListener<T> listener : listeners) {
				listener.onEvent(event, t, e);
			}
		}
	}
}
