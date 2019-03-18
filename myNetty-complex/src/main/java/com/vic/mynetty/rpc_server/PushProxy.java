package com.vic.mynetty.rpc_server;

public class PushProxy {
	
	private static PushService pushService;
	public static boolean inited = false;
	
	public static void init(PushService pushService) {
		PushProxy.pushService = pushService;
		inited = true;
	}
	
	public static <T> T create(Class<T> clz) {
		return (T) pushService.get(clz);
	}
}
