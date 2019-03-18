package com.vic.mynetty.common.factory;

import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.keepalive.HeartbeatKeeper;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;

public interface HeartbeatKeeperFactory {
	HeartbeatKeeper create(HeartbeatStrategy heartbeatStrategy, AbstractSession session);
}
