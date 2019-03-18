package com.vic.mynetty.netty_client;


import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.netty_client.event.SessionEventPropagator;
import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Mode;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.common.event.SessionEventFirer;
import com.vic.mynetty.common.event.SessionEventListener;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.Routable;
import com.vic.mynetty.common.strategyenum.CommunicatorState;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.rpc_common.message.MessageFactory;
import com.vic.mynetty.common.service.AbstractService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * todo 这个类作用，1.生成 session 。 2，通信的同步与异步实现
 */

/**
 * Client side core class, it will open sessions to different servers.
 *
 */
@Slf4j
public class CommunicatorClient extends AbstractService implements Routable<Path, AbstractClientSession> {
    private static final long SLEEP_MILLS = 1;
    private static long lastLogTime;
    // -------- config --------
    /*
     * config that accept user settings
     */
    @Setter
    @Getter
    private ClientSpecialConfig config;
    // -------- sessions --------
    /*
     * sessions
     */
    @Getter
    private AbstractClientSession session;
    /*
     * session event lock to ensure event from different sessions are fired sequentially
     */
    private Object sessionEventlock;
    // -------- event base --------
    /*
     * current communicator state
     */
    @Setter
    @Getter
    private CommunicatorState state;

    /**
     *  session 状态的改变 引起 CommunicatorClient 的改变
     */
    private SessionEventListener communicatorClientChange;

    public CommunicatorClient() {
        this.state = CommunicatorState.NEW;
        this.config = new ClientSpecialConfig();
//        this.sessions = new ArrayList<AbstractClientSession>();
        this.sessionEventlock = new Object();
        this.communicatorClientChange = new SessionEventPropagator(this);
    }

    /**
     * Mainly to open sessions to different servers.
     * Add listener to watch sessions' state change.
     * Propagate sessions' event as communicator event.
     */
    @Override
    protected void startInner() {
        log.info("STARTING_COMMUNICATOR|config=[{}]", this.config);
        // validate session configuration
        config.validate();
        // init listeners
        // get session configurations
        //todo 用配置文件，不用 csv,多个session 连接配置，多个实例
        Discoverer<SessionConfig> discoverer = config.getSessionCfgDiscoverer();
        SessionConfig sessionConfig = discoverer.discover().get(0);

        if (sessionConfig == null) {
            throw new IllegalArgumentException(String.format("NO_SESSION_CONFIGS_FOUND|discoverer=[{}]",
                discoverer.getClass().getName()));
        }
        log.info("SESSION_CONFIGS_DISCOVERERED|sessionConfigs={}|discoverer=[{}]", sessionConfig, discoverer.getClass().getName());
        // create and open session iteratively
        AbstractClientSession session = createSession(sessionConfig);
        session.subscribe(config.getStaticSubscriptions());
        session.forceOpen();
        this.session = session;
    }

    /**
     * End sessions(as well as all connections inside).
     */
    @Override
    protected void stopInner() {
        // close session iteratively
        this.session.forceClose();
        // set null explicitly to avoid holding dropped sessions, and for gc
        session = null;
    }

    private AbstractClientSession createSession(SessionConfig sessionConfig) {
        log.info("CREATING_SESSION|name=[{}]", sessionConfig.getName());
        AbstractClientSession session = this.config.getClientSessionFactory().create(this.config, sessionConfig,
            sessionEventlock);
        session.setUserId(this.config.getUserId());
        log.info("SESSION_CREATED|name=[{}]|routeMatcher=[{}]", session.getName(), session.getRouteMatcher());
        SessionEventFirer eventfirer = session.getEventfirer();
        // add listeners of outer, user defined to session
        //todo  这个session监听器 直接暴露给 用户，让用户定制化
        List<SessionEventListener> sessionListeners = this.config.getSessionListeners();
        if (sessionListeners != null) {
            eventfirer.addListeners(sessionListeners);
        }
        eventfirer.addListener(communicatorClientChange);
        //todo 不要在这里用 sessesionList  让上层（比如 rpc 层 多个session配置生成多个 session）自己去实现list，这层只用通信
        return session;
    }

    // -------- event base --------
    public Communication.Builder newCommBuilder() {
        return new Communication.Builder(this);
    }

    // -------- route --------
    @Override
    public AbstractClientSession route(Path route) {
        if (this.session.getRouteMatcher().matches(route)) {
            return this.session;
        }
        throw new IllegalArgumentException(String.format("ROUTE_NOT_REACHABLE|route=[%s]|session=[%s]",
            route, this.session.getRouteMatcher()));
    }

    private void waitTillReady() {
        while (this.state != CommunicatorState.READY) {
            if (System.currentTimeMillis() - lastLogTime >= 3000) {
                log.info("COMMUNICATOR_NOT_READY|sleep=[{}ms]", SLEEP_MILLS);
                lastLogTime = System.currentTimeMillis();
            }
            try {
                Thread.sleep(SLEEP_MILLS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public <O> O request(Communication communication) throws Exception {
        waitTillReady();
        Mapping.Model mapping = communication.getMapping();
        final AbstractClientSession session = route(mapping.getPath());
        Mode mode = mapping.getMode();
        Retry.Model retry = communication.getRetry();
        final Message reqMsg = MessageFactory.newMessage(communication);
        log.info("NEW_REQUEST|request=[{}]", reqMsg);
        Message respMsg = null;
        switch (mode) {
            case SYNC:
                if (communication.getRetry() != null) {
                    respMsg = session.getRetryDecorator().syncSend4Result(reqMsg, retry);
                } else {
                    respMsg = session.syncSend4Result(reqMsg);
                }
                log.info("RESPONSE_RECEIVED|response=[{}]", respMsg);
                return (O)respMsg.getObjects()[0];
            /**
             * 异步有好几层事件的传递。一般上层
             * todo 这里异步应该想测试类那样，传入future
             */
            case ASYNC:
                if (communication.getRetry() != null) {
                    final Future<Message> futureRespMsg = session.getRetryDecorator().asyncSend4Result(reqMsg, retry);
                    final Future<Object> futureResult = new Future<Object>();
                    futureResult.setRunnable(new Runnable() {
                        @Override
                        public void run() {
                            futureRespMsg.addListener(new FutureListener<Message>() {

                                @Override
                                public void onEvent(FutureEvent event, Message t, Exception e) {
                                    switch (event) {
                                        case SUCCESS:
                                            log.info("RESPONSE_RECEIVED|response=[{}]", t);
                                            futureResult.fireEvent(event, t.getObjects()[0], e);
                                            break;
                                        case FAILURE:
                                            futureResult.fireEvent(event, null, e);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }).begin();
                        }
                    });
                    return (O)futureResult;
                } else {
                    final Future<Message> futureRespMsg = new Future<Message>();
                    final Future<Object> futureResult = new Future<Object>();
                    futureResult.setRunnable(new Runnable() {
                        @Override
                        public void run() {
                            futureRespMsg.addListener(new FutureListener<Message>() {
                                @Override
                                public void onEvent(FutureEvent event, Message t, Exception e) {
                                    switch (event) {
                                        case SUCCESS:
                                            futureResult.fireEvent(event, t.getObjects()[0], e);
                                            break;
                                        case FAILURE:
                                            futureResult.fireEvent(event, null, e);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            });
                            session.asyncSend4Result(reqMsg,futureRespMsg);
                        }
                    });
//                    futureRespMsg.addListener(new FutureListener<Message>() {
//                        @Override
//                        public void onEvent(FutureEvent event, Message t, Exception e) {
//                            switch (event) {
//                                case SUCCESS:
//                                    futureResult.fireEvent(event, t.getObjects()[0], e);
//                                    break;
//                                case FAILURE:
//                                    futureResult.fireEvent(event, null, e);
//                                    break;
//                                default:
//                                    break;
//                            }
//                        }
//                    });
//                    session.asyncSend4Result(reqMsg,futureRespMsg);
                    return (O)futureResult;
                }
            default:
                return null;
        }
    }

    /**
     *  send 是没有返回值的,用于 report
     * @param communication
     * @throws Exception
     */
    public void send(Communication communication) throws Exception {
        waitTillReady();
        final AbstractClientSession session = route(communication.getMapping().getPath());
        Mode mode = communication.getMapping().getMode();
        Retry.Model retry = communication.getRetry();
        final Message message = MessageFactory.newMessage(communication);
        log.info("NEW_REPORT|report=[{}]", message);
        switch (mode) {
            case SYNC:
                if (communication.getRetry() != null) {
                    session.getRetryDecorator().syncSend(message, retry);
                    return;
                }
                //todo 这里应该抛个异常或者打个日志，如果没有标志重试的话，那么就没有发送
            case ASYNC:
                if (communication.getRetry() != null) {
                    session.getRetryDecorator().asyncSend(message, retry);
                    return;
                }
            default:
        }
        session.send(message);
    }

}
