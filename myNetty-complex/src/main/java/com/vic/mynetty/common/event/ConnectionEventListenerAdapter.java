package com.vic.mynetty.common.event;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.message.Message;

public abstract class ConnectionEventListenerAdapter implements ConnectionEventListener {

	@Override
	public void onConnectionActive(Connection connection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionReady(Connection connection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionInactive(Connection connection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionIdle(Connection connection, Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionLost(Connection connection) {
		// TODO Auto-generated method stub
		
	}

}
