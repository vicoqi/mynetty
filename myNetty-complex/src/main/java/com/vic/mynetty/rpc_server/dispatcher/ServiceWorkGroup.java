package com.vic.mynetty.rpc_server.dispatcher;

import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.RouteMatchable;
import com.vic.mynetty.netty_server.config.WorkGroupConfig;
import lombok.Getter;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ServiceWorkGroup implements RouteMatchable<Path> {
	@Getter
	private String name;
	@Getter
	private Path.Matcher routeMatcher;
	private Executor executor;
	
	public ServiceWorkGroup(WorkGroupConfig config) {
		this.routeMatcher = new Path.Matcher(config.getPathMatcher()).compile();
		this.executor = Executors.newFixedThreadPool(config.getThreadCnt());
		this.name = config.getName();
	}
	
	public void handle(Runnable runnable) {
		executor.execute(runnable);
	}
	
}
