package com.vic.mynetty.netty_server.session;


import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.netty_server.config.ServerConfig;
import com.vic.mynetty.netty_server.connection.NettyServerConnection;
import com.vic.mynetty.netty_server.event.ServerConnEventPropagator;
import lombok.Getter;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NettyServerSession extends AbstractSession {
	@Getter
	private int size;
	
	public NettyServerSession(
			ScheduledExecutorService executor,
			String name,
			String id,
			HeartbeatStrategy heartbeatStrategy,
			long allIdleTime,
			TimeUnit unit,
			long timeout,
			ServerConfig serverConfig,
			int size) {
		super(name, 
				heartbeatStrategy, 
				allIdleTime, 
				unit, 
				timeout, 
				serverConfig.getHeartbeatKeeperFactory(),
				null);
		this.scheduledExecutor = executor;
		this.sessionIdleStateMonitor = 
				serverConfig.getSessionIdleStateMonitorFactory().create(this, heartbeatStrategy, allIdleTime, unit, timeout);
		this.id = id;
		this.eventPropagator = new ServerConnEventPropagator(this);
		this.size = size;
		this.eventfirer.addListener(new SessionEventListenerAdapter() {

			@Override
			public void onSessionReady(Session session) {
				NettyServerSession.this.sessionIdleStateMonitor.initialize();
			}

			@Override
			public void onSessionInactive(Session session) {
				NettyServerSession.this.sessionIdleStateMonitor.destroy();
			}
			
		});
	}
	
	public void add(NettyServerConnection connection) {
		if (connection.isHeartbeatConn()) {
			heartbeatConn = connection;
		}
		connection.getEventfirer().addListener(this.eventPropagator);
		connection.getEventfirer().addListener(new ConnectionEventListenerAdapter() {

			@Override
			public void onConnectionInactive(Connection connection) {
				connections.remove(connection);
			}
			
		});
		if (connections == null) {
			connections = new ArrayList<Connection>(size);
		}
		connection.setSession(this);
		connection.getEventfirer().setLock(connEventLock);
		if (connection.getName().equalsIgnoreCase(Connection.DEFAULT_CONN_NAME)) {
			this.defaultConn = connection;
		}
		connections.add(connection);
	}
	
	@Override
	public void open() {
		throw new UnsupportedOperationException("open");
	}
	
	public void remove(NettyServerConnection connection) {
		connections.remove(connection);
	}

}
