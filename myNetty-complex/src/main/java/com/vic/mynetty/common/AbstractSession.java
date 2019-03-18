package com.vic.mynetty.common;


import com.vic.mynetty.common.event.ConnectionEventListener;
import com.vic.mynetty.common.event.SessionEventFirer;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.factory.HeartbeatKeeperFactory;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitor;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.RouteMatcher;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.common.state.SessionState;
import com.vic.mynetty.utils.PrintUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * client session & server session common class
 * @author dlx
 *
 */
@Slf4j
public abstract class AbstractSession implements Session {
	/*
	 * identifiers
	 */
	@Getter
	protected String name;
	@Setter
    @Getter
	protected String id;
	/*
	 * connections
	 */
	@Getter
	protected List<Connection> connections;
	protected Connection heartbeatConn;
	protected Connection defaultConn;
	protected int heartbeatConnIdx = 0;
	/*
	 * state and listeners
	 */
	@Setter
    @Getter
	protected SessionState state = SessionState.NEW;
	@Setter
    @Getter
	protected ConnectionEventListener eventPropagator;
	@Getter
	protected SessionEventFirer eventfirer;
	/*
	 * heart beat settings
	 */
	@Setter
    @Getter
	protected SessionIdleStateMonitor sessionIdleStateMonitor;
	@Setter
    @Getter
	protected HeartbeatStrategy heartbeatStrategy;
	@Setter
    @Getter
	protected long allIdleTime;
	@Setter
    @Getter
	protected TimeUnit idleTimeUnit;
	@Setter
    @Getter
	protected long timeout;
	@Getter
	protected Object connEventLock;
	@Getter
	protected ScheduledExecutorService scheduledExecutor;
	@Setter
    @Getter
	protected String userId;
	@Setter
    @Getter
	protected boolean isHeartTimeOut = false;
	
	public void subscribe(Map<String, FutureListener<?>> staticSubscriptions) {
		for (Map.Entry<String, FutureListener<?>> sub : staticSubscriptions.entrySet()) {
			boolean found = false;
			String pathStr = sub.getKey();
			FutureListener<?> future = sub.getValue();
			Path path = new Path(pathStr);
			for (Connection conn : connections) {
				RouteMatcher<Path> pathMatcher = conn.getRouteMatcher();
				if (pathMatcher != null && pathMatcher.matches(path)) {
					found = true;
					conn.subscribe(pathStr, future);
					break;
				}
			}
			if (!found && defaultConn != null) {
				defaultConn.subscribe(pathStr, future);
			}
		}
	}
	
	public AbstractSession(
			String name,
			HeartbeatStrategy heartbeatStrategy,
			long allIdleTime,
			TimeUnit unit,
			long timeout,
			HeartbeatKeeperFactory heartbeatKeeperFactory,
			Object sessionEventLock) {
		this.name = name;
		this.heartbeatStrategy = heartbeatStrategy;
		this.allIdleTime = allIdleTime;
		this.idleTimeUnit = unit;
		this.timeout = timeout;
		this.connEventLock = new Object();
		// use the delegate to fire events
		this.eventfirer = new SessionEventFirer(this, sessionEventLock);
		// add heart beat keeper, it will listen on idle event
		eventfirer.addListener(heartbeatKeeperFactory.create(heartbeatStrategy, this));
		// add session lost listener, it will listen on session lost event
		eventfirer.addListener(new SessionEventListenerAdapter() {
			
			@Override
			public void onSessionReady(Session session) {
				sessionIdleStateMonitor.initialize();
			}
			
			@Override
			public void onSessionInactive(Session session) {
				sessionIdleStateMonitor.destroy();
			}
			
			@Override
			public void onSessionError(Session session) {
				sessionIdleStateMonitor.destroy();
			}

			@Override
			public void onSessionLost(Session session) {
				//先关闭 channel close ,再触发 channel inactive 事件 -> session inactive 事件
				session.close();
			}
			
		});
	}
	
	public SessionEventFirer getEventfirer() {
		return this.eventfirer;
	}
	
	public Connection getHeartbeatConn() {
		if (this.heartbeatConn == null) {
			// if heart beat connection not set, choose one
			if (heartbeatConnIdx >= connections.size()) {
				heartbeatConnIdx = 0;
			}
			return connections.get(heartbeatConnIdx ++);
		} else {
			// if heart beat connection is set, return the set one
			return heartbeatConn;
		}
	}

	// 关闭 session ,关闭 channel
	public void close() {
        log.debug("CLOSING_SESSION|name=[{}]|id=[{}]",
				this.name, PrintUtil.shortId(this.id, PrintUtil.DELI_STRK_THRU));
		for (Connection connection : connections) {
			connection.close();
		}
	}
	
	@Override
	public Connection route(Path route) {
		for (Connection connection : connections) {
			if (connection.getName().equalsIgnoreCase(Connection.DEFAULT_CONN_NAME)) {
				continue;
			}
			if (connection.getRouteMatcher().matches(route)) {
				return connection;
			}
		}
		return this.defaultConn;
	}
}
