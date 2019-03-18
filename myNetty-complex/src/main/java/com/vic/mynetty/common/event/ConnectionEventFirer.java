package com.vic.mynetty.common.event;


import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.state.ConnectionState;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ConnectionEventFirer {
	private Connection connection;
	@Setter
	private Object lock;
	private List<ConnectionEventListener> listeners;

	public ConnectionEventFirer(Connection connection, Object lock) {
		this.connection = connection;
		this.lock = lock;
	}

	public void addListener(ConnectionEventListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<ConnectionEventListener>();
		}
		listeners.add(listener);
	}

	public void addListeners(List<ConnectionEventListener> listeners) {
		if (this.listeners == null) {
			this.listeners = new ArrayList<ConnectionEventListener>();
		}
		this.listeners.addAll(listeners);
	}

	public void fireConnectionReady() {
		synchronized (lock) {
			connection.setState(ConnectionState.READY);
			if (!CollectionUtils.isEmpty(listeners)) {
				for (ConnectionEventListener listener : listeners) {
					listener.onConnectionReady(connection);
				}
			}
		}
	}

	public void fireConnectionInactive() {
		synchronized (lock) {
			connection.setState(ConnectionState.INACTIVE);
			if (!CollectionUtils.isEmpty(listeners)) {
				for (ConnectionEventListener listener : listeners) {
					listener.onConnectionInactive(connection);
				}
			}
		}
	}

	public void fireConnectionIdle(Message message) {
		synchronized (lock) {
			if (!CollectionUtils.isEmpty(listeners)) {
				for (ConnectionEventListener listener : listeners) {
					listener.onConnectionIdle(connection, message);
				}
			}
		}
	}

	public void fireConnectionLost() {
		synchronized (lock) {
			if (!CollectionUtils.isEmpty(listeners)) {
				for (ConnectionEventListener listener : listeners) {
					listener.onConnectionLost(connection);
				}
			}
		}
	}
}
