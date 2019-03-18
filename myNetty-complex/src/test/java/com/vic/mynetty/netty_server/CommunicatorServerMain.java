package com.vic.mynetty.netty_server;



import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.model.Response;
import com.vic.mynetty.rpc_server.PushProxy;
import com.vic.mynetty.rpc_server.WorkGroupsPropertiesInterpreter;
import com.vic.mynetty.netty_server.api.PushDemo;
import com.vic.mynetty.netty_server.config.WorkGroupConfig;

import java.util.List;

public class CommunicatorServerMain {
	public static void main(String[] args) {
		CommunicatorServer server = new CommunicatorServer();
		server.getConfig().addSessionListener(new SessionEventListenerAdapter() {

			@Override
			public void onSessionReady(Session session) {
				System.out.println("onSessionReady" + " : " + session.getUserId());
			}

			@Override
			public void onSessionInactive(Session session) {
				System.out.println("maintest:onSessionInactive");
			}

			@Override
			public void onSessionLost(Session session) {
				System.out.println("onSessionLost");
			}
			
		})
		.setWorkerCfgDiscoverer(new Discoverer<WorkGroupConfig>() {
			
			@Override
			public List<WorkGroupConfig> discover() {
				return WorkGroupsPropertiesInterpreter.interpret("workGroups-demo.properties", "comm.workGroups");
			}
		})
		.setRequestPackages(new String[]{"com.vic.mynetty.server.api"})
		.setReportPackages(new String[]{"com.vic.mynetty.server.api"})
//		.setPushPackages(new String[]{"com.vic.mynetty.server.api"});
		.setPushPackages(new String[]{"com.vic.mynetty.netty_server.api"});

		server.start();
//		PushDemo pushDemo = PushProxy.create(PushDemo.class);
//		while (true) {
//			pushDemo.push(new Response("push", 1)).unicast("abc");
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
	}
}
