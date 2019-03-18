package com.vic.mynetty.common.event;

import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.state.SessionState;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class SessionEventFirer {
	private Session session;
	private Object lock;
	private List<SessionEventListener> listeners;

	public SessionEventFirer(Session session, Object lock) {
		this.session = session;
		this.lock = lock;
	}

	public void addListener(SessionEventListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<SessionEventListener>();
		}
		listeners.add(listener);
	}

	public void addListeners(List<SessionEventListener> listeners) {
		if (this.listeners == null) {
			this.listeners = new ArrayList<SessionEventListener>();
		}
		this.listeners.addAll(listeners);
	}
	
	public void fireSessionReady() {
		if (lock == null) {
			doFireSessionReady();
		} else {
			synchronized(lock) {
				doFireSessionReady();
			}
		}
	}
	
	private void doFireSessionReady() {
		session.setState(SessionState.READY);
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionReady(session);
			}
		}
	}

	public void fireSessionInactive() {
		if (lock == null) {
			doFireSessionInactive();
		} else {
			synchronized(lock) {
				doFireSessionInactive();
			}
		}
	}
	
	private void doFireSessionInactive() {
		session.setState(SessionState.INACTIVE);
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionInactive(session);
			}
		}
	}

	public void fireSessionIdle(Message message) {
		if (lock == null) {
			doFireSessionIdle(message);
		} else {
			synchronized(lock) {
				doFireSessionIdle(message);
			}
		}
	}
	
	private void doFireSessionIdle(Message message) {
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionIdle(session, message);
			}
		}
	}

	public void fireSessionLost() {
		if (lock == null) {
			doFireSessionLost();
		} else {
			synchronized(lock) {
				doFireSessionLost();
			}
		}
	}
	
	private void doFireSessionLost() {
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionLost(session);
			}
		}
	}
	
	public void fireSessionError() {
		if (lock == null) {
			doFireSessionError();
		} else {
			synchronized(lock) {
				doFireSessionError();
			}
		}
	}
	
	private void doFireSessionError() {
		session.setState(SessionState.ERROR);
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionError(session);
			}
		}
	}
	
	public void fireSessionNetDelay() {
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionNetDelay(session);
			}
		}
	}
	
	public void fireSessionNetRecover() {
		if (!CollectionUtils.isEmpty(listeners)) {
			for (SessionEventListener listener : listeners) {
				listener.onSessionNetRecover(session);
			}
		}
	}
	
}
