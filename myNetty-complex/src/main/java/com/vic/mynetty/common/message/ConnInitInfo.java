package com.vic.mynetty.common.message;

import com.vic.mynetty.netty_client.nettyclient.AbstractClientConnection;
import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class ConnInitInfo {
	// session related
	private String sessionName;
	private String sessionId;
	private int sessionSize;
	private HeartbeatStrategy heartbeatStrategy;
	private long allIdleTime;
	private TimeUnit idleTimeUnit;
	private long timeout;
	// connection related
	private String connName;
	private String connId;
	private boolean isHeartbeatConn;
	private String pathMatcher;
	private boolean defaultConn;
	// user info
	private String userId;
	
	public ConnInitInfo(
			AbstractClientSession session,
			AbstractClientConnection connection) {
		// session related
		this.sessionName = session.getName();
		this.sessionId = session.getId();
		this.sessionSize = session.getConnections().size();
		this.heartbeatStrategy = session.getHeartbeatStrategy();
		this.allIdleTime = session.getAllIdleTime();
		this.idleTimeUnit = session.getIdleTimeUnit();
		this.timeout = session.getTimeout();
		// connection related
		this.connName = connection.getName();
		this.connId = connection.getId();
		this.isHeartbeatConn = connection.isHeartbeatConn();
		this.pathMatcher = connection.getRouteMatcher().getPath();
		this.defaultConn = connection.getName().equalsIgnoreCase(Connection.DEFAULT_CONN_NAME);
		// user info
		this.userId = session.getUserId();
	}
}
