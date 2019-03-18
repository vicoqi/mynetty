package com.vic.mynetty.netty_client.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 每次创建固量的线程池，线程池的中线程的数量，在配置类中配置化好了
 */
public class PooledScheduledExecutorFactory implements ScheduledExecutorFactory {
	
	private int threadCnt;

	public PooledScheduledExecutorFactory(int threadCnt) {
		this.threadCnt = threadCnt;
	}
	
	@Override
	public ScheduledExecutorService create() {
		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(threadCnt);
		return executorService;
	}

}
