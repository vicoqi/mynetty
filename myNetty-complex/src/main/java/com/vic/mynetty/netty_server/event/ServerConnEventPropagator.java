package com.vic.mynetty.netty_server.event;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.state.ConnectionState;
import com.vic.mynetty.netty_server.session.NettyServerSession;

public class ServerConnEventPropagator extends ConnectionEventListenerAdapter {
	private NettyServerSession session;
	
	public ServerConnEventPropagator(NettyServerSession session) {
		this.session = session;
	}

	@Override
	public void onConnectionReady(Connection connection) {
		if (session.getConnections().size() == session.getSize()) {
			for (Connection item : session.getConnections()) {
				if (item.getState() != ConnectionState.READY) {
					return;
				}
			}
			session.getEventfirer().fireSessionReady();
		}
	}

	@Override
	public void onConnectionIdle(Connection connection, Message message) {
		session.getEventfirer().fireSessionIdle(message);
	}
	
	@Override
	public void onConnectionInactive(Connection connection) {
		for (Connection item : session.getConnections()) {
			if (item.getState() != ConnectionState.INACTIVE) {
				return;
			}
		}
		session.getEventfirer().fireSessionInactive();
	}

	@Override
	public void onConnectionLost(Connection connection) {
		session.getEventfirer().fireSessionLost();
	}
	
}
