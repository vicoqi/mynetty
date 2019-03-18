package com.vic.mynetty.netty_client;

import com.vic.mynetty.model.Request;
import com.vic.mynetty.netty_client.api.RequestDemo;
import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.common.strategyenum.OpenConnStrategyEnum;
import com.vic.mynetty.model.Response;
import com.vic.mynetty.rpc_common.proxy.CommunicationDynamicProxy;
import com.vic.mynetty.rpc_client.interpreter.SessionsPropertiesInterpreter;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommunicatorMain {
	public static void main(String[] args) {
		ClientSpecialConfig config = new ClientSpecialConfig();
		config.setSessionCfgDiscoverer(new Discoverer<SessionConfig>() {

			@Override
			public List<SessionConfig> discover() {
				return SessionsPropertiesInterpreter.interpret("sessions-demo.properties", "comm.sessions");
//				return null;
			}
		}).addSessionListener(new SessionEventListenerAdapter() {

			@Override
			public void onSessionReady(Session session) {
				System.out.println(session.getId() + ": onSessionReady");
			}

			@Override
			public void onSessionNetRecover(Session session) {
				System.out.println(session.getId() + ": onSessionNetRecover");
			}

			@Override
			public void onSessionNetDelay(Session session) {
				System.out.println(session.getId() + ": onSessionNetRecover");
			}

			@Override
			public void onSessionLost(Session session) {
				System.out.println(session.getId() + ": onSessionLost");
			}

			@Override
			public void onSessionInactive(Session session) {
				System.out.println(session.getId() + ": onSessionInactive");
			}

			@Override
			public void onSessionIdle(Session session, Message message) {
				System.out.println(session.getId() + ": onSessionIdle");
			}
		}).subscribe("/push/test", new FutureListener<Response>() {

			@Override
			public void onEvent(FutureEvent event, final Response t, Exception e) {
				System.out.println( "接收到服务断push 到的消息："+ t.toString());
			}
		})
		.setUserId("abc")
		.setHeartbeatStrategy(HeartbeatStrategy.CLIENT_INITIATIVE).setIdleTime(5).setIdleTimeUnit(TimeUnit.SECONDS)
				.setTimeout(600).setConnectionOpenStrategy(OpenConnStrategyEnum.ONE_BY_ONE);
		CommunicationDynamicProxy proxy = new CommunicationDynamicProxy(config);
		RequestDemo requestDemo = proxy.createProxy(RequestDemo.class);
		Response resp = requestDemo.sync(new Request("request", 1));
		System.out.println(resp);

//		com.vic.mynetty.common.future.Future<Response> future = requestDemo.async(new Request("request", 2));
//		future.addListener(new FutureListener<Response>() {
//			@Override
//			public void onEvent(FutureEvent event, Response t, Exception e) {
//				System.out.println(t);
////				System.exit(0);
//			}
//			//添加一个 onTimeOutEvent
//		}).begin();
		System.out.println("---------------------END---------------------");
//		ReportDemo reportDemo = CommunicationDynamicProxy.createProxy(ReportDemo.class);
//		reportDemo.schedule(new Request("schedule", 1));
//		reportDemo.retry(new Request("retry", 1));
//		PushRegister register = proxy.createProxy(PushRegister.class);
//		Future<Response> response = register.receive("1");
//		response.addListener(new FutureListener<Response>() {
//			
//			@Override
//			public void onEvent(FutureEvent event, Response t, Exception e) {
//				System.out.println(t);
//			}
//		}).begin();
	}
}
