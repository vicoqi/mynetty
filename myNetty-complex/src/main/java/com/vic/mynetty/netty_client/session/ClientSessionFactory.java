package com.vic.mynetty.netty_client.session;


import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.SessionConfig;

public interface ClientSessionFactory {

	AbstractClientSession create(ClientSpecialConfig commConfig, SessionConfig sessionConfig, Object eventlock);

}
