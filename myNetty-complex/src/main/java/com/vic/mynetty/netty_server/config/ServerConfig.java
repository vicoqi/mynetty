package com.vic.mynetty.netty_server.config;

import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.common.event.SessionEventListener;
import com.vic.mynetty.common.factory.HeartbeatKeeperFactory;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitorFactory;
import com.vic.mynetty.common.nettycoder.codec.Codec;
import com.vic.mynetty.common.nettycoder.codec.ProtostuffCodec;
import com.vic.mynetty.netty_server.nettyserver.ConnectionAcceptorFactory;
import com.vic.mynetty.netty_server.keepalive.NettyServerSessionIdleStateMonitorFactory;
import com.vic.mynetty.netty_server.keepalive.ServerHeartbeatKeeperFactory;
import com.vic.mynetty.netty_server.nettyserver.NettyTCPConnectionAcceptorFactory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *   服务端 RPC 调用相关的配置类，
 * 1.api 在那些包下，需要扫描
 * 2. RPC 调用的解编码器
 * 3. 端口
 *
 */
public class ServerConfig {
	@Getter
	private String[] pushPackages;
	@Getter
	private String[] requestPackages;
	@Getter
	private String[] reportPackages;
	@Getter
	private Codec codec = new ProtostuffCodec();
	@Getter
	private SessionIdleStateMonitorFactory sessionIdleStateMonitorFactory = new NettyServerSessionIdleStateMonitorFactory();
	@Getter
	private HeartbeatKeeperFactory heartbeatKeeperFactory = new ServerHeartbeatKeeperFactory();
	//netty 通信端口
	@Getter
	private int port = 8091;
	@Getter
	private ConnectionAcceptorFactory connectionAcceptorFactory = new NettyTCPConnectionAcceptorFactory();
	@Getter
	private List<SessionEventListener> sessionListeners;
	@Getter
	private Discoverer<WorkGroupConfig> workerCfgDiscoverer;
	
	public ServerConfig setPushPackages(String[] packageNames) {
		this.pushPackages = packageNames;
		return this;
	}
	
	public ServerConfig setRequestPackages(String[] packageNames) {
		this.requestPackages = packageNames;
		return this;
	}
	
	public ServerConfig setReportPackages(String[] packageNames) {
		this.reportPackages = packageNames;
		return this;
	}
	
	public ServerConfig setCodec(Codec codec) {
		this.codec = codec;
		return this;
	}
	
//	public ServerConfig setSessionIdleStateFactory(SessionIdleStateMonitorFactory sessionIdleStateMonitorFactory) {
//		this.sessionIdleStateMonitorFactory = sessionIdleStateMonitorFactory;
//		return this;
//	}
//
//	public ServerConfig setHeartbeatKeeperFactory(HeartbeatKeeperFactory heartbeatKeeperFactory) {
//		this.heartbeatKeeperFactory = heartbeatKeeperFactory;
//		return this;
//	}
	
	public ServerConfig setPort(int port) {
		this.port = port;
		return this;
	}
//	public ServerConfig setConnectionAcceptorFactory(ConnectionAcceptorFactory connectionAcceptorFactory) {
//		this.connectionAcceptorFactory = connectionAcceptorFactory;
//		return this;
//	}
	
	public ServerConfig addSessionListener(SessionEventListener sessionListener) {
		if (this.sessionListeners == null) {
			this.sessionListeners = new ArrayList<SessionEventListener>();
		}
		this.sessionListeners.add(sessionListener);
		return this;
	}
	
	public ServerConfig addSessionListeners(List<SessionEventListener> sessionListeners) {
		if (this.sessionListeners == null) {
			this.sessionListeners = new ArrayList<SessionEventListener>();
		}
		this.sessionListeners.addAll(sessionListeners);
		return this;
	}
	
	public ServerConfig setWorkerCfgDiscoverer(Discoverer<WorkGroupConfig> workerCfgDiscoverer) {
		this.workerCfgDiscoverer = workerCfgDiscoverer;
		return this;
	}
}
