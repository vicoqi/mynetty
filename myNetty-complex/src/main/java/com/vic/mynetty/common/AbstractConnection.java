package com.vic.mynetty.common;


import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.event.ConnectionEventFirer;
import com.vic.mynetty.common.event.ConnectionEventListenerAdapter;
import com.vic.mynetty.common.exception.RemoteCallException;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.common.future.FutureMapListener;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.future.TimingFutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.qoos.NetworkAnalyzer;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.state.ConnectionState;
import com.vic.mynetty.utils.PrintUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Client side and Server side common class.
 *
 * @author dlx
 */
@Slf4j
public abstract class AbstractConnection implements Connection {
    // -------- identifiers --------
    /*
     *
     */
    @Setter
    @Getter
    protected String name;
    @Setter
    @Getter
    protected String id;
    @Setter
    @Getter
    protected String sessionName;
    @Setter
    @Getter
    protected String sessionId;
    // -------- event base --------
    /*
     *
     */
    @Setter
    @Getter
    protected ConnectionState state;
    protected ConnectionEventFirer eventfirer;
    // -------- heart beat --------
    /*
     *
     */
    @Setter
    @Getter
    protected boolean heartbeatConn;
    // -------- route matcher --------
    /*
     *
     */
    @Setter
    @Getter
    protected Path.Matcher routeMatcher;
    // -------- request & response bind --------
    /*
     *
     */
    @Getter
    protected Map<String, TimingFutureListener<Message>> futureRespMsgsMap;
    @Setter
    @Getter
    protected ScheduledExecutorService executor;
    protected NetworkAnalyzer networkAnalyzer;
    protected Map<String, FutureListener<?>> staticSubscriptions = new HashMap<String, FutureListener<?>>();
    private Object putAndClearLock = new Object();
    private long latestExpire = -1;
    private Runnable task = new Runnable() {

        @Override
        public void run() {
            long curTm = System.currentTimeMillis();
            for (String key : futureRespMsgsMap.keySet()) {
                TimingFutureListener<Message> listener = futureRespMsgsMap.get(key);
                long expireTm = listener.getExpireTm();
                if (expireTm == -1) {
                    continue;
                }
                if (expireTm < curTm) {
                    futureRespMsgsMap.remove(key).onEvent(FutureEvent.FAILURE, null, new TimeoutException());
                    log.info("REQUEST_TIMEOUT_THEN_DROPED|chnnlId=[{}]|reqMsgId=[{}]|expireTm=[{}]|curTm=[{}]",
                        PrintUtil.shortId(id, PrintUtil.DELI_STRK_THRU),
                        PrintUtil.shortId(key, PrintUtil.DELI_STRK_THRU), expireTm, curTm);
                    if (networkAnalyzer != null) {
                        networkAnalyzer.analysis(Long.MAX_VALUE);
                    }
                } else {
                    if (latestExpire <= curTm) {
                        latestExpire = listener.getExpireTm();
                    } else {
                        if (listener.getExpireTm() < latestExpire) {
                            latestExpire = listener.getExpireTm();
                        }
                    }
                }
            }
            long nextDelay = latestExpire - curTm;
            executor.schedule(task, nextDelay < 0 ? 1000 : nextDelay, TimeUnit.MILLISECONDS);
        }
    };

    protected AbstractConnection(String id, Object eventlock) {
        this.id = id;
        this.eventfirer = new ConnectionEventFirer(this, eventlock);
        this.state = ConnectionState.NEW;
    }

    public AbstractConnection(
        String name,
        String sessionName,
        String sessionId,
        String id,
        boolean heartbeatConn,
        String pathMatcher,
        Object eventlock) {
        this(id, eventlock);
        this.sessionName = sessionName;
        this.sessionId = sessionId;
        this.name = name;
        this.heartbeatConn = heartbeatConn;
        this.routeMatcher = new Path.Matcher(pathMatcher);
        this.routeMatcher.compile();
        this.eventfirer.addListener(new ConnectionEventListenerAdapter() {

            @Override
            public void onConnectionInactive(Connection connection) {
                synchronized (putAndClearLock) {
                    if (futureRespMsgsMap != null) {
                        for (String key : futureRespMsgsMap.keySet()) {
                            TimingFutureListener<Message> listener = futureRespMsgsMap.get(key);
                            listener.onEvent(FutureEvent.TIMEOUT, null,
                                new TimeoutException("SERVER_CONNECTION_CLOSED"));
                        }
                    }
                }
            }

        });
    }

    public <T> void subscribe(String path, FutureListener<T> future) {
        staticSubscriptions.put(path, future);
    }

    public ConnectionEventFirer getEventfirer() {
        return this.eventfirer;
    }

    // -------- request & response bind --------
    @Override
    public void bind(Message reqMessage, TimingFutureListener<Message> respMsg) {
        if (futureRespMsgsMap == null) {
            futureRespMsgsMap = new ConcurrentHashMap<String, TimingFutureListener<Message>>();
        }
        String bindId = reqMessage.getId();
        if (reqMessage.getMapping() != null && reqMessage.getMapping().getType() == Type.PUSH) {
            bindId = (String)reqMessage.getObjects()[0];
        }
        synchronized (putAndClearLock) {
            if (this.state == ConnectionState.INACTIVE) {
                respMsg.onEvent(FutureEvent.TIMEOUT, null, new TimeoutException("SERVER_CONNECTION_CLOSED"));
            } else {
                futureRespMsgsMap.put(bindId, respMsg);
            }
        }
        long expireTm = respMsg.getExpireTm();
        if (expireTm == -1) {
            return;
        }
        if (latestExpire == -1) {
            synchronized (this) {
                if (latestExpire == -1) {
                    latestExpire = expireTm;
                    long firstDelay = latestExpire - System.currentTimeMillis();
                    executor.schedule(task, firstDelay, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public void onReceive(final Message message,ChannelHandlerContext ctx) {
        String collaborateId = message.getCollaborationId();
        FutureListener<Message> future;
        if (message.getMapping() != null && message.getMapping().getType() == Type.PUSH) {
            String pathStr = message.getMapping().getPath().getPath();
            final FutureListener<Object> subscriptionFuture = (FutureListener<Object>)staticSubscriptions.get(pathStr);
            if (subscriptionFuture != null) {
//                ctx.writeAndFlush(); // todo 响应成功
                subscriptionFuture.onEvent(FutureEvent.SUCCESS,
                    message.getObjects() == null ? null : message.getObjects()[0], null);
            }else{
//                ctx.writeAndFlush(); //todo 响应失败，没有接口
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

    public void send(Message message,FutureMapListener<Message> futureResult){

    }

}
