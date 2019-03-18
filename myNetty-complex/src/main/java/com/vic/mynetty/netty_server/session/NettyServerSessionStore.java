package com.vic.mynetty.netty_server.session;


import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.netty_server.config.ServerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyServerSessionStore {
	private ServerConfig serverConfig;
	private Object lock = new Object();
	
	public NettyServerSessionStore(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	
	@Getter
	private Map<String, NettyServerSession> id2SesionMap = 
			new ConcurrentHashMap<String, NettyServerSession>();
	@Getter
	private Map<String, NettyServerSession> userId2SesionMap = 
			new ConcurrentHashMap<String, NettyServerSession>();
	
	public NettyServerSession query(String sessionId) {
		return id2SesionMap.get(sessionId);
	}
	
	public NettyServerSession queryByUserId(String userId) {
		return userId2SesionMap.get(userId);
	}
	
	public NettyServerSession create(
			ScheduledExecutorService executor,
			final String sessionName,
			final String sessionId,
			HeartbeatStrategy heartbeatStrategy,
			long allIdleTime,
			TimeUnit idleTimeUnit,
			long timeouts,
			int connCnt,
			String userId) {
		final NettyServerSession session = new NettyServerSession(
				executor,
				sessionName, 
				sessionId, 
				heartbeatStrategy, 
				allIdleTime, 
				idleTimeUnit, 
				timeouts, 
				serverConfig,
				connCnt);
		/* 
		 * The session creation process will fail when the following conditions exists:
		 * 1) The session with the userId is already in the NettyServerSessionStore
		 * 2) The the sesion of the existed session doesn't equal to the session id we want to created.
		 */
		synchronized(lock) {
			NettyServerSession querySession = queryByUserId(userId);
			if(querySession != null) {
				String querySessionId = querySession.getId();
				if(!querySessionId.equalsIgnoreCase(sessionId)) {
					log.info("NettyServerSessionStore|create nettyServerSession|failed due to existing session|oldSessionID=[{}]|want create sessionID=[{}]", querySessionId, sessionId);
					return null;
				} else {
					// This situation should not exist
					log.info("NettyServerSessionStore|create nettyServerSession|this situation shouldn't exit|oldSessionID=[{}]|want create sessionID=[{}]", querySessionId, sessionId);
					return null;
				}
			} else {
				session.setUserId(userId);
				id2SesionMap.put(sessionId, session);
				userId2SesionMap.put(userId, session);
			}
		}
		// remove from map when session inactive
		session.getEventfirer().addListener(new SessionEventListenerAdapter() {

			@Override
			public void onSessionInactive(Session session) {
				id2SesionMap.remove(session.getId());
				userId2SesionMap.remove(session.getUserId());
			}
			
		});
		session.getEventfirer().addListeners(serverConfig.getSessionListeners());
		return session;
	}
	
}
