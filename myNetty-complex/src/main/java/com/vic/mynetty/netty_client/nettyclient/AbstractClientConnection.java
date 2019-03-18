package com.vic.mynetty.netty_client.nettyclient;

import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.ConnectionConfig;
import com.vic.mynetty.common.AbstractConnection;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.exception.RemoteCallException;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.message.MessageFactory;
import com.vic.mynetty.common.qoos.BackOffCalculator;
import com.vic.mynetty.common.qoos.NetworkAnalyzer;
import com.vic.mynetty.common.qoos.TrafficRegulator;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.utils.PrintUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractClientConnection extends AbstractConnection {
	@Getter
	protected BackOffCalculator reconnectBackOffCalc;
	@Getter
	protected long initTimeout;
	
	public AbstractClientConnection(
			ClientSpecialConfig commConfig,
			String sessionName,
			String sessionId,
			ConnectionConfig config,
			TrafficRegulator trafficRegulator,
			NetworkAnalyzer networkAnalyzer,
			Object eventlock) {
		super(config.getName(), 
				sessionName,
				sessionId,
				null,
				config.isHeartbeatConn(), 
				config.getPathMatcher(),
				eventlock);
		this.reconnectBackOffCalc = new BackOffCalculator(
				commConfig.getReconnectStrategy(), 
				commConfig.getReconnectParameters(), 
				trafficRegulator);
		this.networkAnalyzer = networkAnalyzer;
		this.initTimeout = commConfig.getInitTimeout();
		this.eventfirer.addListener(new ConnectionEventListenerAdapter() {

			@Override
			public void onConnectionReady(Connection connection) {
				reconnectBackOffCalc.reset();
			}
			
		});
	}

	@Override
	public void onReceive(final Message message, ChannelHandlerContext ctx) {
		String collaborateId = message.getCollaborationId();
		FutureListener<Message> future;
		if (message.getMapping() != null && message.getMapping().getType() == Type.PUSH) {
			String pathStr = message.getMapping().getPath().getPath();
			final FutureListener<Object> subscriptionFuture = (FutureListener<Object>)staticSubscriptions.get(pathStr);
			Message pushOKResp = MessageFactory.newRespMessage(message,null);
			if (subscriptionFuture != null) {
                ctx.writeAndFlush(pushOKResp); // todo 响应成功
				subscriptionFuture.onEvent(FutureEvent.SUCCESS,
						message.getObjects() == null ? null : message.getObjects()[0], null);
			}else{
//                ctx.writeAndFlush(); //todo 响应失败，没有接口
				pushOKResp.setErrorMsg("Not finding the suitable path");
				ctx.writeAndFlush(pushOKResp);
			}
		} else {
			future = futureRespMsgsMap.remove(collaborateId);
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

}
