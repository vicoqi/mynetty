package com.vic.mynetty.netty_client.scheduled;

import java.util.concurrent.ScheduledExecutorService;

public interface ScheduledExecutorFactory {
	ScheduledExecutorService create();
}
