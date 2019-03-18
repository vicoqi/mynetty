package com.vic.mynetty.netty_server.nettyserver;

import com.vic.mynetty.common.nettycoder.codec.Codec;
import com.vic.mynetty.netty_server.ServerContext;

public interface ConnectionAcceptorFactory {
	ConnectionAcceptor create(Codec codec, int port, ServerContext serverContext);
}
