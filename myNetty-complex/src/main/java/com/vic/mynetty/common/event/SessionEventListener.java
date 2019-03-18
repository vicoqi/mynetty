package com.vic.mynetty.common.event;

import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.message.Message;

public interface SessionEventListener {
	void onSessionReady(Session session);
	void onSessionNetDelay(Session session);
	void onSessionNetRecover(Session session);
	void onSessionInactive(Session session);
	void onSessionIdle(Session session, Message message);
	void onSessionLost(Session session);
	void onSessionError(Session session);
}
