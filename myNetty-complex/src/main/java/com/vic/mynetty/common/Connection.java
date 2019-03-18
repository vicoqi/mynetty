package com.vic.mynetty.common;


import com.vic.mynetty.common.event.ConnectionEventFirer;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.future.TimingFutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.RouteMatchable;
import com.vic.mynetty.common.state.ConnectionState;
import io.netty.channel.ChannelHandlerContext;

public interface Connection extends RouteMatchable<Path> {
	String DEFAULT_CONN_NAME = "default";
	/*
	 * 
	 */
	String getName();
	String getId();
	String getSessionName();
	String getSessionId();
	void setSessionName(String sessionName);
	void setSessionId(String sessionId);
	/*
	 * 
	 */
	void open();
	void close();
	void send(Message message);
	void bind(Message reqMessage, TimingFutureListener<Message> respMessage);
	void onReceive(Message message,ChannelHandlerContext ctx);
	/*
	 * 
	 */
	ConnectionState getState();
	void setState(ConnectionState state);
	ConnectionEventFirer getEventfirer();
	
	<T> void subscribe(String path, FutureListener<T> future);
}
