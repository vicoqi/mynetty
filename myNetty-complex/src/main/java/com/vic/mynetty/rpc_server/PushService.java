package com.vic.mynetty.rpc_server;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.rpc_common.message.MessageFactory;
import com.vic.mynetty.rpc_common.service.AbstractServerService;
import com.vic.mynetty.rpc_common.service.Publisher;
import com.vic.mynetty.netty_server.ServerContext;
import lombok.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PushService extends AbstractServerService {
	private Map<Class<?>, Object> clz2InstanceMap = new HashMap<Class<?>, Object>();
	private Map<Mapping.Model, Map<String, Connection>> mapping2ReceiversMap =
			new HashMap<Mapping.Model, Map<String, Connection>>();
	@Setter
	private ServerContext serverContext;
	
	@Override
	protected boolean isServiceClazz(Class<?> candidate) {
		if (!candidate.isInterface()) {
			return false;
		}
		Mapping mapping = candidate.getAnnotation(Mapping.class);
		if (mapping == null) {
			return false;
		}
		Type typeValue = mapping.type();
        return typeValue == Type.PUSH;
	}

	@Override
	protected void initCandidate(final Class<?> candidate) {
		Object instance = Proxy.newProxyInstance(PushService.class.getClassLoader(), new Class<?>[] { candidate },
				new InvocationHandler() {

					@Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
						Mapping.Model mapping = new Mapping.Interpreter().interpret(candidate, method);
						return publish(mapping, args);
					}
				});
		clz2InstanceMap.put(candidate, instance);
	}

	private Publisher publish(Mapping.Model mapping, Object[] args) {
		Message message = MessageFactory.newPushMessage(mapping, args);
		return new Publisher(message, serverContext.getServerSessionStore().getId2SesionMap().values(),serverContext.getFutureResult());
	}
	
	private Object lock = new Object();
	public void register(Mapping.Model mapping, String unitcastId, Connection connection) {
		Map<String, Connection> receivers = mapping2ReceiversMap.get(mapping);
		if (receivers == null) {
			synchronized(lock) {
				if (receivers == null) {
                    receivers = new ConcurrentHashMap<>();
					mapping2ReceiversMap.put(mapping, receivers);
				}
			}
		}
		receivers.put(unitcastId, connection);
	}
	
	public void unregister(Connection connection) {
		for (Mapping.Model mapping : mapping2ReceiversMap.keySet()) {
			if (connection.getRouteMatcher().matches(mapping.getPath())) {
				Map<String, Connection> receivers = mapping2ReceiversMap.get(mapping);
				Iterator<Entry<String, Connection>> entrySet = receivers.entrySet().iterator();
				while (entrySet.hasNext()) {
					Entry<String, Connection> entry = entrySet.next();
					if (entry.getValue().getId().equals(connection.getId())) {
						entrySet.remove();
					}
				}
			}
		}
	}

	@Override
	protected String[] getScanningPackages() {
		return serverContext.getServerConfig().getPushPackages();
	}

	@Override
	protected void stopInner() {
		// TODO Auto-generated method stub
		
	}
	
	public Object get(Class<?> clz) {
		return clz2InstanceMap.get(clz);
	}
}
