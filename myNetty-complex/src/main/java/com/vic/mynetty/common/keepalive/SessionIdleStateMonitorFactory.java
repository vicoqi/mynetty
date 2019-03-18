package com.vic.mynetty.common.keepalive;

import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;

import java.util.concurrent.TimeUnit;

public interface SessionIdleStateMonitorFactory {
	SessionIdleStateMonitor create(AbstractSession session, HeartbeatStrategy heartbeatStrategy, long allIdleTime, TimeUnit unit, long timeout);
}
