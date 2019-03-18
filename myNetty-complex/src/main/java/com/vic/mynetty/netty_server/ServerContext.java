package com.vic.mynetty.netty_server;

import com.vic.mynetty.common.future.FutureMapListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.rpc_server.PushService;
import com.vic.mynetty.rpc_server.ReportService;
import com.vic.mynetty.rpc_server.RequestService;
import com.vic.mynetty.rpc_server.dispatcher.ServiceWorkGroupDispatcher;
import com.vic.mynetty.netty_server.config.ServerConfig;
import com.vic.mynetty.netty_server.connection.NettyServerConnectionStore;
import com.vic.mynetty.netty_server.session.NettyServerSessionStore;
import lombok.Getter;

public class ServerContext {
	@Getter
	private ServerConfig serverConfig;
	@Getter
	private PushService pushService;
	@Getter
	private RequestService requestService;
	@Getter
	private ReportService reportService;
	@Getter
	private NettyServerConnectionStore serverConnectionStore;
	@Getter
	private NettyServerSessionStore serverSessionStore;
	@Getter
	private ServiceWorkGroupDispatcher serviceDispatcher;

	//放置监听器的地方
	@Getter
	private  FutureMapListener<Message> futureResult = new FutureMapListener<>();

	
	public void onConfigReady(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	
	public void onServicesReady(
			PushService pushService,
			RequestService requestService,
			ReportService reportService) {
		this.pushService = pushService;
		this.pushService.setServerContext(this);
		this.requestService = requestService;
		this.reportService = reportService;
	}
	
	public void onStoresReady() {

		this.serverConnectionStore = new NettyServerConnectionStore();
		this.serverSessionStore = new NettyServerSessionStore(this.getServerConfig());

//		this.serverConnectionStore = serverConnectionStore;
//		this.serverSessionStore = serverSessionStore;
//		this.pushService.setServerSessionStore(serverSessionStore);
	}
	
	public void onDispatcherReady(
			ServiceWorkGroupDispatcher serviceDispatcher) {
		this.serviceDispatcher = serviceDispatcher;
	}
	
	public void destroy() {
		this.serverConfig = null;
		this.pushService = null;
		this.requestService = null;
		this.reportService = null;
	}
}
