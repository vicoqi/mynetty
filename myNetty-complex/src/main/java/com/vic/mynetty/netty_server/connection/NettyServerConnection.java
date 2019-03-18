package com.vic.mynetty.netty_server.connection;

import com.google.common.cache.LoadingCache;
import com.vic.mynetty.common.AbstractConnection;
import com.vic.mynetty.common.exception.RemoteCallException;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.future.FutureMapListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.netty_server.session.NettyServerSession;
import com.vic.mynetty.utils.PrintUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class NettyServerConnection extends AbstractConnection {
	@Getter
	private Channel channel;
	@Setter
    @Getter
	private NettyServerSession session;

	private LoadingCache<String, FutureListener<Message>> expireListenerCache;

	@Getter
	protected Map<String, FutureListener<Message>> connectRespFuture = new HashMap<>();


	private void cachePut(String key,FutureListener<Message> value){
		expireListenerCache.cleanUp();
		expireListenerCache.put(key,value);
	}

	public NettyServerConnection(Channel channel,LoadingCache<String, FutureListener<Message>> expireListenerCache) {
		super(channel.id().asLongText(), null);
		this.channel = channel;
		this.expireListenerCache = expireListenerCache;
	}
	
	public NettyServerConnection(
			String name, 
			String id, 
			String sessionName,
			String sessionId,
			boolean heartbeatConn, 
			String pathMatcher) {
		super(name, id, sessionName, sessionId, heartbeatConn, pathMatcher, null);
	}
	
	@Override
	public void open() {
		throw new UnsupportedOperationException();
	}
	@Override
	public void close() {
		channel.close();
	}

	@Override
	public void send(Message message) {
		channel.writeAndFlush(message);
	}

	/**
	 * 服务端异步发送任务
	 * @param message
	 * @param futureResult
	 */
	@Override
	public void send(Message message, FutureMapListener<Message> futureResult) {
		FutureListener<Message> timmingFuture = new FutureListener<Message>() {
			//超时，失败，成功，都会出发这个事件
			@Override
			public void onEvent(FutureEvent event, Message t, Exception e) {
				connectRespFuture.remove(t.getCollaborationId());
				expireListenerCache.invalidate(t.getCollaborationId());
				//删除并且驱动上一层
				futureResult.getListeners().remove(t.getCollaborationId()).onEvent(event, t, null);
			}
		};
		cachePut(message.getId(),timmingFuture);  //超时事件
		connectRespFuture.put(message.getId(),timmingFuture);  //成功或者失败事件
		channel.writeAndFlush(message);
	}

	@Override
    public void onReceive(final Message message,ChannelHandlerContext ctx) {
        String collaborateId = message.getCollaborationId();
        FutureListener<Message> future;
		future = connectRespFuture.remove(collaborateId);
		if (future != null) {
			if (!StringUtils.isEmpty(message.getErrorMsg())) {
				future.onEvent(FutureEvent.FAILURE, message, new RemoteCallException(message.getErrorMsg()));
			} else {
				if (message.getNetStartTm() != 0 && message.getNetEndTm() != 0) {
					long netCostMills = message.getNetEndTm() - message.getNetStartTm();
					log.info("NET_COST|chnnlId=[{}]|msgId=[{}]|coMsgId=[{}]|cost=[{}ms]",
							PrintUtil.shortId(id, PrintUtil.DELI_STRK_THRU),
							PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU),
							PrintUtil.shortId(message.getCollaborationId(), PrintUtil.DELI_STRK_THRU),
							netCostMills / 1000000);
					if (networkAnalyzer != null) {
						networkAnalyzer.analysis(netCostMills);
					}
				}
				future.onEvent(FutureEvent.SUCCESS, message, null);
			}
		}
    }

}
