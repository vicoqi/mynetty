package com.vic.mynetty.netty_client.session;


import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.netty_client.nettyclient.NettyClientSession;

public class NettyClientSessionFactory implements ClientSessionFactory {

	@Override
	public NettyClientSession create(ClientSpecialConfig commConfig, SessionConfig sessionConfig, Object sessionEventLock) {
		return new NettyClientSession(commConfig, sessionConfig, sessionEventLock);
	}

}
