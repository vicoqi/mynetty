package com.vic.mynetty.netty_server.connection;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.future.TimingFutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class NettyServerConnectionStore {
	private Map<String, NettyServerConnection> ctx2ConnectionMap = new ConcurrentHashMap<String, NettyServerConnection>();

	private RemovalListener<String, FutureListener<Message>> listener = notification ->
	{
		if (notification.wasEvicted()) {
			RemovalCause cause = notification.getCause();
			log.info("remove cacase is :" + cause.toString());
			Message message = new Message();
			message.setCollaborationId(notification.getKey());
			notification.getValue().onEvent(FutureEvent.TIMEOUT, message, new TimeoutException("SERVER_PUSH_TIMEOUT"));
			log.info("remove messageId:" + notification.getKey());
		}
	};

	// 泛型String 就是 messageId
	private LoadingCache<String, FutureListener<Message>> expireListenerCache = CacheBuilder.newBuilder()
			.expireAfterAccess(4000, TimeUnit.MILLISECONDS)
//			.expireAfterWrite(1000, TimeUnit.MILLISECONDS)
			.removalListener(listener)// 添加删除监听
			.build(createCacheLoader());

	private com.google.common.cache.CacheLoader<String, FutureListener<Message>> createCacheLoader() {
		return new com.google.common.cache.CacheLoader<String, FutureListener<Message>>() {
			@Override
			public TimingFutureListener<Message> load(String key) throws Exception{
				log.info("加载创建key:" + key);
				return null;
			}
		};
	}


	public NettyServerConnection create(final Channel channel) {
		NettyServerConnection serverConnection = new NettyServerConnection(channel,expireListenerCache);
		serverConnection.getEventfirer().addListener(new ConnectionEventListenerAdapter() {
			
			@Override
			public void onConnectionInactive(Connection connection) {
				NettyServerConnection nettyServerConn = (NettyServerConnection) connection;
				String channelId = nettyServerConn.getChannel().id().asLongText();
				ctx2ConnectionMap.remove(channelId);
			}
			
		});
		ctx2ConnectionMap.put(channel.id().asLongText(), serverConnection);
		return serverConnection;
	}

	public NettyServerConnection query(Channel channel) {
		return ctx2ConnectionMap.get(channel.id().asLongText());
	}

}
