package com.vic.mynetty.netty_server.keepalive;

import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitor;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitorFactory;
import com.vic.mynetty.common.keepalive.netty.NettySessionIdleStateMonitor;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;

import java.util.concurrent.TimeUnit;

public class NettyServerSessionIdleStateMonitorFactory implements SessionIdleStateMonitorFactory {

	@Override
	public SessionIdleStateMonitor create(
			AbstractSession session,
			HeartbeatStrategy heartbeatStrategy,
			long allIdleTime, 
			TimeUnit unit,
			long timeout) {
		if (heartbeatStrategy == HeartbeatStrategy.SERVER_INITIATIVE) {
			// will fire idle event after allIdleTime ms
			return new NettySessionIdleStateMonitor(session, allIdleTime, unit);
		} else {
			// will fire lost event after allIdleTime * timeout ms
			return new NettySessionIdleStateMonitor(session, allIdleTime * timeout, unit);
		}
	}

}
