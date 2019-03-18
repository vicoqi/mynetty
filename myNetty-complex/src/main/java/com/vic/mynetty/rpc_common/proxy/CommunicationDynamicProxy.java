package com.vic.mynetty.rpc_common.proxy;


import com.vic.mynetty.netty_client.Communication;
import com.vic.mynetty.netty_client.CommunicatorClient;
import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Retry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class CommunicationDynamicProxy {
	private Mapping.Interpreter mappingInterpreter = new Mapping.Interpreter();
	private Retry.Interpreter retryInterpreter = new Retry.Interpreter();
//	private Schedule.Interpreter scheduleInterpreter = new Schedule.Interpreter();
	private CommunicatorClient communicator;
	
	public CommunicationDynamicProxy(CommunicatorClient communicator) {
		this.communicator = communicator;
		this.communicator.start();
	}
	
	public CommunicationDynamicProxy(ClientSpecialConfig commConfig) {
		this.communicator = new CommunicatorClient();
		this.communicator.setConfig(commConfig);
		this.communicator.start();
	}
	
	public void destory() {
		communicator.stop();
	}
	
	@SuppressWarnings("unchecked")
	public <C> C createProxy(final Class<C> clz) {
		return (C) Proxy.newProxyInstance(clz.getClassLoader(), new Class<?>[]{clz}, new InvocationHandler() {
			
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Mapping.Model mapping = mappingInterpreter.interpret(clz, method);
				Retry.Model retry = retryInterpreter.interpret(clz, method);
//				Schedule.Model schedule = scheduleInterpreter.interpret(clz, method);
				Communication communication = new Communication.Builder(communicator)
						.setMapping(mapping)
						.setRetry(retry)
//						.setSchedule(schedule)
						.setReturnType(method.getGenericReturnType())
						.build();
				switch (mapping.getType()) {
				case REQUEST:
					return communication.request(args);
				case PUSH:
//					return communication.receive((String) args[0]);
				case REPORT:
					communication.send(args);
					return null;
				default:
					throw new UnsupportedOperationException("type=" + mapping.getType());
				}
			}
		});
	}
}
