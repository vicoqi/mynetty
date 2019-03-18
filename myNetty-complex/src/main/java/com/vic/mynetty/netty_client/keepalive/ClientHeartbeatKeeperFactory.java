package com.vic.mynetty.netty_client.keepalive;

import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.factory.HeartbeatKeeperFactory;
import com.vic.mynetty.common.keepalive.HeartbeatKeeper;
import com.vic.mynetty.common.keepalive.InitialtiveHeartbeatKeeper;
import com.vic.mynetty.common.keepalive.PassiveHeartbeatKeeper;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;

public class ClientHeartbeatKeeperFactory implements HeartbeatKeeperFactory {
	public HeartbeatKeeper create(HeartbeatStrategy heartbeatStrategy, AbstractSession session) {
		if (heartbeatStrategy == HeartbeatStrategy.CLIENT_INITIATIVE) {
			return new InitialtiveHeartbeatKeeper(session);
		} else {
			return new PassiveHeartbeatKeeper(session);
		}
	}
}
