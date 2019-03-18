package com.vic.mynetty.common.service;

public class ServiceStatus {
	public static final int INITIALIZED = 0, STARTING = 1, STARTED = 2, SHUTTING_DOWN = 3, SHUTTED_DOWN = 0;

	public static boolean canStartup(int serviceStatus) {
		if (serviceStatus == INITIALIZED) {
			return true;
		}
		return false;
	}

	public static boolean canShutdown(int serviceStatus) {
		if (serviceStatus == STARTED) {
			return true;
		}
		return false;
	}
}
