package com.vic.mynetty.netty_client.event;


import com.vic.mynetty.netty_client.CommunicatorClient;
import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.strategyenum.CommunicatorState;

public class SessionEventPropagator extends SessionEventListenerAdapter {
	private CommunicatorClient communicator;
	
	public SessionEventPropagator(CommunicatorClient communicator) {
		this.communicator = communicator;
	}

	@Override
	public void onSessionReady(Session session) {
		communicator.setState(CommunicatorState.READY);
	}

	@Override
	public void onSessionInactive(Session session) {
		communicator.setState(CommunicatorState.INACTIVE);
	}

}
