package com.vic.mynetty.common.service;

public abstract class AbstractService implements Service {
	protected int serviceStatus = ServiceStatus.INITIALIZED;

	@Override
	public synchronized void start() {
		if (!ServiceStatus.canStartup(serviceStatus)) {
			throw new IllegalStateException();
		}
		serviceStatus = ServiceStatus.STARTING;
		startInner();
		serviceStatus = ServiceStatus.STARTED;
	}

	protected abstract void startInner();

	@Override
	public synchronized void stop() {
		if (!ServiceStatus.canShutdown(serviceStatus)) {
			throw new IllegalStateException();
		}
		serviceStatus = ServiceStatus.STARTING;
		stopInner();
		serviceStatus = ServiceStatus.STARTED;
	}

	protected abstract void stopInner();

	@Override
	public synchronized void restart() {
		stop();
		start();
	}
}
