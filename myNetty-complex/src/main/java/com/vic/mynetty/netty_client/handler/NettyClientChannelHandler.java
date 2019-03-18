package com.vic.mynetty.netty_client.handler;

import com.vic.mynetty.netty_client.nettyclient.NettyClientConnection;
import com.vic.mynetty.netty_client.nettyclient.NettyClientSession;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class NettyClientChannelHandler extends SimpleChannelInboundHandler<Message> {

    private NettyClientSession nettyTCPSession;
    private HeartbeatStrategy heartbeatStrategy;

    public NettyClientChannelHandler(
        NettyClientSession nettyTCPSession) {
        this.nettyTCPSession = nettyTCPSession;
        this.heartbeatStrategy = nettyTCPSession.getHeartbeatStrategy();
    }

    /**
     * operationComplete fired first, then channelActive
     */
    @Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		/*NettyTCPClientConnection nettyTCPConn = (NettyTCPClientConnection) nettyTCPSession.query(ctx.channel());
		initMessage = MessageFactory.newInitMessage(nettyTCPSession, nettyTCPConn);
		logger.info("CHANNEL_ACTIVE_SENDING_INIT_MESSAGE|chnnlId=[{}]|initMsg=[{}]", 
				PrintUtil.shortId(nettyTCPConn.getId(), PrintUtil.DELI_STRK_THRU), initMessage);
		ctx.writeAndFlush(initMessage);*/
		log.info("CHANNEL_ACTIVE|chnnlId=[{}]", ctx.channel().id().asShortText());
		super.channelActive(ctx);
	}

    /**
     * connect -> exception -> close -> inactive
     * connect -> idle -> close -> inactive
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyClientConnection nettyTCPConn = (NettyClientConnection)nettyTCPSession.query(ctx.channel());
        log.info("CHANNEL_INACTIVE|chnnlId=[{}]", ctx.channel().id().asShortText());
        nettyTCPConn.getEventfirer().fireConnectionInactive();
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        msg.setNetEndTm(System.nanoTime());
        NettyClientConnection nettyTCPConn = (NettyClientConnection)nettyTCPSession.query(ctx.channel());
        switch (msg.getMessageType()) {
            case INIT:
                String coId = msg.getCollaborationId();
                Map<String, Message> pendingInitMessages = nettyTCPSession.getPendingInitMessages();
                Message pendingInitMessage = pendingInitMessages.get(coId);
                if (pendingInitMessage != null) {
                    pendingInitMessages.remove(coId);
                    nettyTCPConn.getEventfirer().fireConnectionReady();
                } else {
                    log.info("IGNORE_RESPONSE_INIT_MESSAGE|respInitMsg=[{}]", msg);
                }
                break;
            case HEART_BEAT:
                if (heartbeatStrategy == HeartbeatStrategy.CLIENT_INITIATIVE) {
                    nettyTCPConn.onReceive(msg,null);
                } else {
                    nettyTCPConn.getEventfirer().fireConnectionIdle(msg);
                }
                break;
            case DATA:
                nettyTCPConn.onReceive(msg,ctx);
                break;
            default:
                break;
        }
    }

    /**
     * connect -> exception -> close
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String channelId = ctx.channel().id().asShortText();
        log.error(String.format("EXCEPTION_CAUGHT|chnnlId=[%s]", channelId), cause);
        if (cause instanceof IOException) {
            log.info("SERVER_CLOSED_CLOSING_CHANNEL|chnnlId=[{}]", channelId);
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        NettyClientConnection nettyTCPConn = (NettyClientConnection)nettyTCPSession.query(ctx.channel());
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
            switch (idleStateEvent.state()) {
                case ALL_IDLE:
                    if (heartbeatStrategy == HeartbeatStrategy.CLIENT_INITIATIVE) {
                        nettyTCPConn.getEventfirer().fireConnectionIdle(null);
                    } else {
                        log.error("LOST_HEART_BEAT");
                        nettyTCPConn.getEventfirer().fireConnectionLost();
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
