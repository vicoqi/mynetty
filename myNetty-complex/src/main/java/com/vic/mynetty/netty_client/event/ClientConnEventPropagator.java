package com.vic.mynetty.netty_client.event;

import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.state.ConnectionState;
import com.vic.mynetty.common.state.SessionState;

public class ClientConnEventPropagator extends ConnectionEventListenerAdapter {
	private static final int ALL_NEW_STATE = 1;
	private static final int HALF_NEW_STATE = 2;
	private static final int ERROR_STATE = 3;
	
	private AbstractClientSession session;

	public ClientConnEventPropagator(AbstractClientSession session) {
		this.session = session;
	}
	
	private int checkState(SessionState sessionState,
			ConnectionState oldConnState,
			ConnectionState newConnState) {
		if (session.getState() != sessionState) {
			return ERROR_STATE;
		}
		for (Connection connection : session.getConnections()) {
			ConnectionState connState = connection.getState();
			if (connState == oldConnState) {
				return HALF_NEW_STATE;
			} else {
				if (connState != newConnState) {
					return ERROR_STATE;
				}
			}
		}
		return ALL_NEW_STATE;
	}
	
	private int checkState(SessionState sessionState, 
			ConnectionState connState) {
		if (session.getState() != sessionState) {
			return ERROR_STATE;
		}
		for (Connection connection : session.getConnections()) {
			if (connection.getState() != connState) {
				return ERROR_STATE;
			}
		}
		return ALL_NEW_STATE;
	}

	@Override
	public void onConnectionReady(Connection connection) {
		int ret = checkState(SessionState.NEW, ConnectionState.NEW, ConnectionState.READY);
		switch (ret) {
		case ALL_NEW_STATE:
			session.getEventfirer().fireSessionReady();
			break;
		case HALF_NEW_STATE:
			if (session.hasNext2Open()) {
				session.openNext();
			}
			break;
		case ERROR_STATE:
			if (session.getState() != SessionState.ERROR) {
				session.getEventfirer().fireSessionError();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onConnectionInactive(Connection connection) {
		if (session.getState() == SessionState.ERROR) {
			return;
		}
		int ret = checkState(SessionState.READY, ConnectionState.READY, ConnectionState.INACTIVE);
		switch (ret) {
		case ALL_NEW_STATE:
			session.getEventfirer().fireSessionInactive();
			break;
		case HALF_NEW_STATE:
			session.close();
			break;
		case ERROR_STATE:
			if (session.getState() != SessionState.ERROR) {
				session.getEventfirer().fireSessionError();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onConnectionIdle(Connection connection, Message message) {
		int ret = checkState(SessionState.READY, ConnectionState.READY);
		switch (ret) {
		case ALL_NEW_STATE:
			session.getEventfirer().fireSessionIdle(message);
			break;
		case ERROR_STATE:
			if (session.getState() != SessionState.ERROR) {
				session.getEventfirer().fireSessionError();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onConnectionLost(Connection connection) {
		int ret = checkState(SessionState.READY, ConnectionState.READY);
		switch (ret) {
		case ALL_NEW_STATE:
			session.getEventfirer().fireSessionLost();
			break;
		case ERROR_STATE:
			if (session.getState() != SessionState.ERROR) {
				session.getEventfirer().fireSessionError();
			}
			break;
		default:
			break;
		}
	}

}
