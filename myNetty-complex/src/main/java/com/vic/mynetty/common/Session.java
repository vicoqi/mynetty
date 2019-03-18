package com.vic.mynetty.common;

import com.vic.mynetty.common.event.SessionEventFirer;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitor;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.Routable;
import com.vic.mynetty.common.state.SessionState;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Session extends Routable<Path, Connection> {
	String getName();
	String getId();
	void setId(String id);
	String getUserId();
	/*
	 * 
	 */
	SessionState getState();
	void setState(SessionState state);
	/*
	 * 
	 */
	void open();
	void close();
	/*
	 * 
	 */
	List<Connection> getConnections();
	Connection getHeartbeatConn();
	/*
	 * 
	 */
	SessionEventFirer getEventfirer();
	/*
	 * 
	 */
	SessionIdleStateMonitor getSessionIdleStateMonitor();
	long getAllIdleTime();
	TimeUnit getIdleTimeUnit();
	long getTimeout();
	boolean isHeartTimeOut();
}
