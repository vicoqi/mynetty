package com.vic.mynetty.netty_server.handler;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.keepalive.netty.NettyConnIdleStateHandler;
import com.vic.mynetty.common.keepalive.netty.NettySessionIdleStateMonitor;
import com.vic.mynetty.common.message.ConnInitInfo;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.message.MessageFactory;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.rpc_server.ReportService;
import com.vic.mynetty.rpc_server.RequestService;
import com.vic.mynetty.rpc_server.dispatcher.ServiceWorkGroup;
import com.vic.mynetty.rpc_server.dispatcher.ServiceWorkGroupDispatcher;
import com.vic.mynetty.netty_server.ServerContext;
import com.vic.mynetty.netty_server.connection.NettyServerConnection;
import com.vic.mynetty.netty_server.connection.NettyServerConnectionStore;
import com.vic.mynetty.netty_server.session.NettyServerSession;
import com.vic.mynetty.netty_server.session.NettyServerSessionStore;
import com.vic.mynetty.utils.PrintUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @Date: 2018/9/30 11:23
 * @Description:
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private NettyServerConnectionStore serverConnectionStore;
    private NettyServerSessionStore serverSessionStore;

    private ServerContext serverContext;
//
    private RequestService requestService;
    private ReportService reportService;
//    private PushService pushService;
    private ServiceWorkGroupDispatcher serviceDispatcher;

    public NettyServerHandler(ServerContext serverContext) {
        this.serverContext = serverContext;
        this.serverConnectionStore = serverContext.getServerConnectionStore();
        this.serverSessionStore = serverContext.getServerSessionStore();
        this.requestService = serverContext.getRequestService();
        this.reportService = serverContext.getReportService();
//        this.pushService = serverContext.getPushService();
        this.serviceDispatcher = serverContext.getServiceDispatcher();
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("CHANNEL_ACTIVE|chnnlId=[{}]", ctx.channel().id().asLongText());
        NettyServerConnection serverConnection = serverConnectionStore.create(ctx.channel());
        serverConnection.getEventfirer().addListener(new ConnectionEventListenerAdapter() {

            @Override
            public void onConnectionInactive(Connection connection) {
//                pushService.unregister(connection);
            }

        });
    }

    /**
     * client progress closure will trigger inactive message
     * if client's net down -> app close -> net up, still will accept this message
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("CHANNEL_INACTIVE|chnnlId=[{}]", ctx.channel().id().asShortText());
        NettyServerConnection connection = serverConnectionStore.query(ctx.channel());
        if (connection != null) {
            if(connection.getSession().isHeartTimeOut()){
                log.debug("CHANNEL_INACTIVE|heartTimeOut|chnnlId=[{}]", ctx.channel().id().asShortText());
            }else{
                log.debug("CHANNEL_INACTIVE|normal|chnnlId=[{}]", ctx.channel().id().asShortText());
            }
            connection.getEventfirer().fireConnectionInactive();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        NettyServerConnection serverConnection = serverConnectionStore.query(ctx.channel());
        switch (message.getMessageType()) {
            case INIT:
                init(ctx, serverConnection, message);
                break;
            case HEART_BEAT:
                heartbeat(serverConnection, message);
                break;
            case DATA:
                log.debug("DATA_READ|id=[{}]", message.getId());
                //根据mapping 划分线程池,线程池的个数和大小，来自配置文件
                ServiceWorkGroup worker = serviceDispatcher.route(message.getMapping().getPath());
                worker.handle(new Runnable() {
                    @Override
                    public void run() {
                        Type type = message.getMapping().getType();
                        switch (type) {
                            case REQUEST:
                                request(ctx, message);
                                break;
                            case REPORT:
                                report(ctx, message);
                                break;
                            case PUSH:
                                push(ctx, message);
                                break;
                            default:
                                break;
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
        ctx.fireChannelReadComplete();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(String.format("CHANNEL_EXCEPTION|channlId=[%s]", ctx.channel().id().asShortText()), cause);
        if (cause instanceof IOException) {
            ctx.close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
//        NettyServerConnection serverConnection = serverConnectionStore.query(ctx.channel());
//        NettyServerSession serverSession = serverConnection.getSession();
//        HeartbeatStrategy heartbeatStrategy = serverSession.getHeartbeatStrategy();
//        if (evt instanceof IdleStateEvent) {
//            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
//            switch (idleStateEvent.state()) {
//                case ALL_IDLE:
//                    if (heartbeatStrategy == HeartbeatStrategy.CLIENT_INITIATIVE) {
//                        log.error("LOST_HEART_BEAT");
//                        serverConnection.eventfirer().fireConnectionLost();
//                    } else {
//                        serverConnection.eventfirer().fireConnectionIdle(null);
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
    }

    private void init(ChannelHandlerContext ctx, NettyServerConnection serverConn, Message message) {
        Object[] objects = message.getObjects();
        ConnInitInfo connInitInfo = (ConnInitInfo) objects[0];
        String connId = connInitInfo.getConnId();
        String connName = connInitInfo.getConnName();
        String sessionName= connInitInfo.getSessionName();
        String sessionId= connInitInfo.getSessionId();
        Boolean isHeartbeatConn = connInitInfo.isHeartbeatConn();
        HeartbeatStrategy heartbeatStrategy = connInitInfo.getHeartbeatStrategy();
        long idleTime = connInitInfo.getAllIdleTime();
        TimeUnit idleTimeUnit = connInitInfo.getIdleTimeUnit();
        long timeout = connInitInfo.getTimeout();
        String pathMatcher = connInitInfo.getPathMatcher();
        int connCnt = connInitInfo.getSessionSize();
        String userId = connInitInfo.getUserId();
        serverConn.setName(connName);
        serverConn.setId(connId);
        serverConn.setSessionName(sessionName);
        serverConn.setSessionId(sessionId);
        serverConn.setRouteMatcher(new Path.Matcher(pathMatcher).compile());
        serverConn.setHeartbeatConn(isHeartbeatConn);

        NettyServerSession session = serverSessionStore.query(sessionId);
        if (session == null) {
            session = serverSessionStore.create(ctx.executor(), sessionName, sessionId, heartbeatStrategy, idleTime, idleTimeUnit, timeout, connCnt, userId);
            // If the creation process of the expected session is failed, just close this user session and return.
            if(session == null) {
                NettyServerSession queryUserSession = serverSessionStore.queryByUserId(userId);
                if(queryUserSession != null) {
                    queryUserSession.close();
                }
                String queryUserSessionId = queryUserSession==null?"null":queryUserSession.getId();
                log.info("INIT_CONNECTION_FAILED|oldSessionID=[{}]|new sessionID=[{}]", queryUserSessionId, sessionId);
                return;
            }
            session.setUserId(userId);
        }
        session.add(serverConn);
        // add heartbeat monitor
        NettyConnIdleStateHandler idleStateHandler = new NettyConnIdleStateHandler(
                (NettySessionIdleStateMonitor) session.getSessionIdleStateMonitor());
        ctx.pipeline().addBefore(ctx.name(), null, idleStateHandler);
        // response
        Message respInitMsg = MessageFactory.newRespInitMessage(message);
        ctx.writeAndFlush(respInitMsg);
        serverConn.getEventfirer().fireConnectionReady();
    }

    //调用上层处理 request 类型
    private void request(ChannelHandlerContext ctx, Message message) {
        log.debug("REQUEST_START|id=[{}]", message.getId());
        long startTm = System.currentTimeMillis();
        Object result = null;
        Exception respException = null;
        try {
            result = serverContext.getRequestService().request(message.getMapping(), message.getObjects());
        } catch (Exception e) {
            log.error(String
                            .format("exception_accured|inMsgId=[%s]", PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU)),
                    e);
            respException = e;
        } finally {
            long endTm = System.currentTimeMillis();
            long costMills = endTm - startTm;
            long respTimeoutMills = message.getRespTimeoutMillis();
            if (respTimeoutMills == -1 || (costMills) < message.getRespTimeoutMillis()) {
                Message respMsg = MessageFactory.newRespMessage(message, new Object[]{result});
                respMsg.setNetEndTm(startTm);
                if (respException != null) {
                    respMsg.setErrorMsg(respException.getMessage());
                }
                ctx.writeAndFlush(respMsg);
                log.warn("REQUEST_END|realCost=[{}ms]|timeout=[{}ms]|id=[{}]|coId[{}]|path=[{}]", costMills, message.getRespTimeoutMillis(), message.getId(), message.getCollaborationId(),message.getMapping().getPath().toString());
            } else {
                log.warn("TIMEOUT_THEN_DROP_RESPONSE|realCost=[{}ms]|timeout=[{}ms]", costMills,
                        message.getRespTimeoutMillis());
            }
        }
    }

    /**
     * 收到心跳的响应
     * @param serverConnection
     * @param message
     */
    private void heartbeat(NettyServerConnection serverConnection, Message message) {
        HeartbeatStrategy heartbeatStrategy = serverConnection.getSession().getHeartbeatStrategy();
//        switch (heartbeatStrategy) {
//            case SERVER_INITIATIVE:
//                serverConnection.onReceive(message);
//                break;
//            case CLIENT_INITIATIVE:
                serverConnection.getEventfirer().fireConnectionIdle(message);
//                break;
//            default:
//                break;
//        }
    }

    /***
     *
     * @param ctx
     * @param message
     */
    private void report(ChannelHandlerContext ctx, Message message) {
        log.debug("REPORT_START|id=[{}]", message.getId());
        reportService.report(message.getMapping(), message.getObjects());
        log.debug("REPORT_END|id=[{}]", message.getId());
    }

    /**
     *
     * @param ctx
     * @param message
     */
    private void push(final ChannelHandlerContext ctx, final Message message) {
        log.debug("PUSH_ONRECEIVE|id=[{}]", message.getCollaborationId());
        NettyServerConnection nettyTCPConn = serverConnectionStore.query(ctx.channel());
        nettyTCPConn.onReceive(message,ctx);
    }
}
