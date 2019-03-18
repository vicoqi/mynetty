package com.vic.mynetty.common.event;


import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.message.Message;

public abstract class SessionEventListenerAdapter implements SessionEventListener {

	@Override
	public void onSessionReady(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionNetDelay(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionNetRecover(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionInactive(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionIdle(Session session, Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionLost(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionError(Session session) {
		// TODO Auto-generated method stub
		
	}

}
