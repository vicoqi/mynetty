package com.vic.mynetty.netty_server.nettyserver;


import com.vic.mynetty.common.nettycoder.codec.Codec;
import com.vic.mynetty.common.service.Service;
import com.vic.mynetty.netty_server.ServerContext;

public interface ConnectionAcceptor extends Service {
	void setCodec(Codec codec);
	void setPort(int port);
	void setServerContext(ServerContext serverContext);
}