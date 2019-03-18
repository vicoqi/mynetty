package com.vic.mynetty.netty_client.nettyclient;


import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.event.ClientConnEventPropagator;
import com.vic.mynetty.netty_client.scheduled.ScheduledExecutorFactory;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.event.SessionEventListenerAdapter;
import com.vic.mynetty.common.exception.RemoteCallException;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.common.message.MessageType;
import com.vic.mynetty.common.route.RouteMatchable;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.common.future.TimingFutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.state.ConnectionState;
import com.vic.mynetty.common.state.SessionState;
import com.vic.mynetty.common.strategyenum.OpenConnStrategyEnum;
import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.decorators.RetryDecorator;
import com.vic.mynetty.common.qoos.NetworkAnalyzer;
import com.vic.mynetty.common.qoos.TrafficRegulator;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.utils.PrintUtil;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.beanutils.BeanUtils;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class AbstractClientSession extends AbstractSession implements RouteMatchable<Path> {
    /*
     * server address
     */
    @Getter
    protected String host;
    @Getter
    protected int port;
    /*
     * connection open strategy
     */
    protected OpenConnStrategyEnum connOpenStrategy;
    // used when connOpenStrategy is ONE_BY_ONE
    // should reset to 0 when server open
    protected volatile int connIdx = 0;
    /*
     * network traffic regulation
     */
    @Getter
    protected TrafficRegulator trafficRegulator;
    /*
     * route matcher that determine whether current session can handle incoming bucketId
     */
    @Getter
    protected Path.Matcher routeMatcher;
    /*
     * decorators
     */
    @Getter
    protected RetryDecorator retryDecorator;
//    @Getter
//    protected ScheduleDecorator scheduleDecorator;
    @Getter
    protected NetworkAnalyzer networkAnalyzer;
    protected boolean forceClosed = false;
    private long sleepMills = 3000;

    public AbstractClientSession(
        ClientSpecialConfig commConfig,
        SessionConfig sessionConfig,
        Object sessionEventLock) {
        super(sessionConfig.getName(),
            commConfig.getHeartbeatStrategy(),
            commConfig.getIdleTime(),
            commConfig.getIdleTimeUnit(),
            commConfig.getTimeout(),
            commConfig.getHeartbeatKeeperFactory(),
            sessionEventLock);
        this.userId = commConfig.getUserId();
        this.eventPropagator = new ClientConnEventPropagator(this);
        // init server address
        this.host = sessionConfig.getHost();
        this.port = sessionConfig.getPort();
        // init connection open strategy setting
        this.connOpenStrategy = commConfig.getConnOpenStrategy();
        this.networkAnalyzer = new NetworkAnalyzer(this, commConfig);
        // init network traffic regulation setting
        this.trafficRegulator = new TrafficRegulator(
            commConfig.getTrafficRegulationStrategy(),
            commConfig.getTrafficRegulationParameters());
        ScheduledExecutorFactory scheduledExecutorFactory = commConfig.getScheduledExecutorFactory();
        if (scheduledExecutorFactory != null) {
            this.scheduledExecutor = scheduledExecutorFactory.create();
        }
        this.sessionIdleStateMonitor =
            commConfig.getSessionIdleStateMonitorFactory().create(this, heartbeatStrategy, allIdleTime, idleTimeUnit,
                timeout);
        this.retryDecorator = new RetryDecorator(this, trafficRegulator, scheduledExecutor);
//        this.scheduleDecorator = new ScheduleDecorator(this, trafficRegulator, scheduledExecutor);
        this.eventfirer.addListener(this.trafficRegulator);
        // reconnect when inactive
        this.eventfirer.addListener(new SessionEventListenerAdapter() {

            @Override
            public void onSessionInactive(Session session) {
                session.open();
            }

            @Override
			public void onSessionError(Session session) {
				session.open();
			}
        });
    }

    public void open() {
        if (this.forceClosed) {
            return;
        }
        this.state = SessionState.NEW;
        this.connIdx = 0;
        this.id = UUID.randomUUID().toString();
        for (Connection connection : connections) {
            connection.setState(ConnectionState.NEW);
        }
        log.debug("OPENNING_SESION|name=[{}]|sessnId=[{}]", this.name, this.id);
        switch (connOpenStrategy) {
            case ONE_BY_ONE:
                openNext();
                break;
            case ALL_AT_ONCE:
                openAll();
                break;
            default:
                break;
        }
    }

    public boolean hasNext2Open() {
        return (this.connIdx < (connections.size()))
            && (connOpenStrategy == OpenConnStrategyEnum.ONE_BY_ONE);
    }

    public void openNext() {
        Connection curConn = connections.get(connIdx++);
        curConn.setSessionId(this.id);
        curConn.open();
    }

    public void openAll() {
        for (Connection connection : connections) {
            connection.open();
        }
    }

    public void forceOpen() {
        this.forceClosed = false;
        open();
    }

    public void forceClose() {
        this.forceClosed = true;
        close();
    }

    private void waitTillReady() {
        while (this.state != SessionState.READY) {
            log.info("SESSION_NOT_READY|sessnId=[{}]|sleep=[{}ms]",
                PrintUtil.shortId(this.id, PrintUtil.DELI_STRK_THRU), sleepMills);
            try {
                Thread.sleep(sleepMills);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Message syncSend4Result(final Message message) throws Exception {
        waitTillReady();
        final Thread currentThread = Thread.currentThread();
        final Message result = new Message();
        final ExceptionWrapper exceptionWrapper = new ExceptionWrapper();
        Connection connection = route(message.getMapping().getPath());
        TimingFutureListener<Message> timmingFuture = new TimingFutureListener<Message>() {

            @Override
            public void onEvent(FutureEvent event, Message t, Exception e) {
                if (e != null) {
                    exceptionWrapper.setException(e);
                }
                if (t != null) {
                    try {
                        BeanUtils.copyProperties(result, t);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                currentThread.interrupt();
            }
        };
        long expireTm = System.currentTimeMillis() + message.getRespTimeoutMillis();
        timmingFuture.setExpireTm(expireTm);
        log.debug("MESSAGE_EXPIRE_INFO|msgId=[{}]|expireTm=[{}]",
            PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU), expireTm);
        connection.bind(message, timmingFuture);
        connection.send(message);
        try {
            Thread.sleep(message.getRespTimeoutMillis());
        } catch (InterruptedException e) {
            Exception exception = exceptionWrapper.getException();
            if (exception != null) {
                throw exception;
            }
            if (!StringUtils.isEmpty(result.getErrorMsg())) {
                throw new RemoteCallException(result.getErrorMsg());
            }
            return result;
        }
        throw new TimeoutException(
            String.format("TIMEOUT|msgId=[%s]|timeout=[%sms]", message.getId(), message.getRespTimeoutMillis()));
    }

    public Future<Message> asyncSend4Result(final Message message,Future<Message> future) {
        final Connection connection = route(message.getMapping().getPath());
//        final Future<Message> future = new Future<Message>();
        final long expireTm = System.currentTimeMillis() + message.getRespTimeoutMillis();
//        future.setRunnable(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    waitTillReady();
//                    Connection conn = connection;
//                    if (message.getMessageType() == MessageType.HEART_BEAT) {
//                        conn = getHeartbeatConn();
//                    }
//                    TimingFutureListener<Message> timmingFuture = new TimingFutureListener<Message>() {
//                        @Override
//                        public void onEvent(FutureEvent event, Message t, Exception e) {
//                            future.fireEvent(event, t, e);
//                        }
//                    };
//                    timmingFuture.setExpireTm(expireTm);
//                    conn.bind(message, timmingFuture);
//                    conn.send(message);
//                } catch (Exception e) {
//                    future.fireEvent(FutureEvent.FAILURE, null, e);
//                }
//            }
//        });
        waitTillReady();
        Connection conn = connection;
        if (message.getMessageType() == MessageType.HEART_BEAT) {
            conn = getHeartbeatConn();
        }
        TimingFutureListener<Message> timmingFuture = new TimingFutureListener<Message>() {
            @Override
            public void onEvent(FutureEvent event, Message t, Exception e) {
                future.fireEvent(event, t, e);
            }
        };
        timmingFuture.setExpireTm(expireTm);
        conn.bind(message, timmingFuture);
        conn.send(message);
        return future;
    }

    public void send(Message message) {
        waitTillReady();
        Connection connection = route(message.getMapping().getPath());
        connection.send(message);
    }

    @Data
    private static class ExceptionWrapper {
        private Exception exception;
    }

}
