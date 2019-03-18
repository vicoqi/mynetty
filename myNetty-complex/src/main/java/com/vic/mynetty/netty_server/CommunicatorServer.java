package com.vic.mynetty.netty_server;

import com.vic.mynetty.rpc_server.PushProxy;
import com.vic.mynetty.rpc_server.PushService;
import com.vic.mynetty.rpc_server.ReportService;
import com.vic.mynetty.rpc_server.RequestService;
import com.vic.mynetty.rpc_server.dispatcher.ServiceWorkGroupDispatcher;
import com.vic.mynetty.netty_server.config.ServerConfig;
import com.vic.mynetty.netty_server.nettyserver.ConnectionAcceptor;
import com.vic.mynetty.common.service.AbstractService;
import lombok.Getter;
import lombok.Setter;

public class CommunicatorServer extends AbstractService {
	@Setter
    @Getter
	private ServerConfig config = new ServerConfig();
	@Getter
	private PushService pushService;
	@Getter
	private RequestService requestService;
	@Getter
	private ReportService reportService;
	private ServerContext serverContext = new ServerContext();
	private ConnectionAcceptor connectionAcceptor;
	@Getter
	private ServiceWorkGroupDispatcher serviceDispatcher;

	@Override
	protected void startInner() {
		serverContext.onConfigReady(config);
		
		serviceDispatcher = new ServiceWorkGroupDispatcher(config);
		serverContext.onDispatcherReady(serviceDispatcher);
		
		pushService = new PushService();
		pushService.setServerContext(serverContext);
		pushService.start();

		
		requestService = new RequestService();
		requestService.setServerContext(serverContext);
		requestService.start();
		
		reportService = new ReportService();
		reportService.setServerContext(serverContext);
		reportService.start();
		
		serverContext.onServicesReady(pushService, requestService, reportService);

		connectionAcceptor = config.getConnectionAcceptorFactory().create(config.getCodec(), config.getPort(), serverContext);
		connectionAcceptor.setServerContext(serverContext);
		connectionAcceptor.start();

		PushProxy.init(pushService);
	}

	@Override
	protected void stopInner() {
		serverContext.destroy();
		connectionAcceptor.stop();
	}
}
