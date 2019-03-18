package com.vic.mynetty.rpc_server.dispatcher;

import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.Routable;
import com.vic.mynetty.netty_server.config.ServerConfig;
import com.vic.mynetty.netty_server.config.WorkGroupConfig;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceWorkGroupDispatcher implements Routable<Path, ServiceWorkGroup> {
	
	private static final String DEFAULT_WORK_GROUP_NAME = "default";
	private ServiceWorkGroup defaultWorkGroup;
	private List<ServiceWorkGroup> workGroups;
			
	public ServiceWorkGroupDispatcher(ServerConfig serverConfig) {
		List<WorkGroupConfig> workerCfgs = serverConfig.getWorkerCfgDiscoverer().discover();
		if (CollectionUtils.isEmpty(workerCfgs)) {
			throw new IllegalArgumentException(String.format("NO_DISPATCH_WORK_CONFIGS_FOUND|discoverer", 
					serverConfig.getWorkerCfgDiscoverer().getClass().getName()));
		}
		workGroups = new ArrayList<ServiceWorkGroup>(workerCfgs.size());
		for (WorkGroupConfig workerCfg : workerCfgs) {
			ServiceWorkGroup workGroup = new ServiceWorkGroup(workerCfg);
			if (workGroup.getName().equals(DEFAULT_WORK_GROUP_NAME)) {
				this.defaultWorkGroup = workGroup;
				continue;
			}
			workGroups.add(workGroup);
		}
	}

	@Override
	public ServiceWorkGroup route(Path route) {
		for (ServiceWorkGroup serviceWorker : workGroups) {
			if (serviceWorker.getRouteMatcher().matches(route)) {
				return serviceWorker;
			}
		}
		return defaultWorkGroup;
	}

	
	public static void main(String[] args) {
		ServerConfig serverConfig = new ServerConfig();
		serverConfig.setWorkerCfgDiscoverer(new Discoverer<WorkGroupConfig>() {
			
			@Override
			public List<WorkGroupConfig> discover() {
				List<WorkGroupConfig> configs = new ArrayList<>();
				WorkGroupConfig c1 = new WorkGroupConfig();
				c1.setPathMatcher("/a/**");
				c1.setThreadCnt(5);
				WorkGroupConfig c2 = new WorkGroupConfig();
				c2.setPathMatcher("/b/**");
				c2.setThreadCnt(5);
				configs.add(c1);
				configs.add(c2);
				return configs;
			}
		});
		ServiceWorkGroupDispatcher dispatcher = new ServiceWorkGroupDispatcher(serverConfig);
		System.out.println(dispatcher.route(new Path("/1")).getRouteMatcher());
	}
}
