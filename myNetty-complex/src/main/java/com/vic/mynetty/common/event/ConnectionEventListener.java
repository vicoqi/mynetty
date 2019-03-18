package com.vic.mynetty.common.event;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.message.Message;

public interface ConnectionEventListener {
	void onConnectionActive(Connection connection);
	void onConnectionReady(Connection connection);
	void onConnectionInactive(Connection connection);
	void onConnectionIdle(Connection connection, Message heartbeat);
	void onConnectionLost(Connection connection);
}
