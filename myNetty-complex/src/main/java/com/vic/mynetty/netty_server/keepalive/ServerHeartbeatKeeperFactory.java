package com.vic.mynetty.netty_server.keepalive;


import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.factory.HeartbeatKeeperFactory;
import com.vic.mynetty.common.keepalive.HeartbeatKeeper;
import com.vic.mynetty.common.keepalive.PassiveHeartbeatKeeper;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;

public class ServerHeartbeatKeeperFactory implements HeartbeatKeeperFactory {
	@Override
	public HeartbeatKeeper create(HeartbeatStrategy heartbeatStrategy, AbstractSession session) {
		return new PassiveHeartbeatKeeper(session);
	}
//	public HeartbeatKeeper create(HeartbeatStrategy heartbeatStrategy, AbstractSession session) {
//		if (heartbeatStrategy == HeartbeatStrategy.SERVER_INITIATIVE) {
//			return new InitialtiveHeartbeatKeeper(session);
//		} else {
//			return new PassiveHeartbeatKeeper(session);
//		}
//	}
}
