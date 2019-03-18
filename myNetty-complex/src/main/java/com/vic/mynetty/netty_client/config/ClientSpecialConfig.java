package com.vic.mynetty.netty_client.config;

import com.vic.mynetty.netty_client.keepalive.ClientHeartbeatKeeperFactory;
import com.vic.mynetty.netty_client.keepalive.NettyClientSessionIdleStateMonitorFactory;
import com.vic.mynetty.netty_client.scheduled.PooledScheduledExecutorFactory;
import com.vic.mynetty.netty_client.scheduled.ScheduledExecutorFactory;
import com.vic.mynetty.netty_client.session.ClientSessionFactory;
import com.vic.mynetty.netty_client.session.NettyClientSessionFactory;
import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.common.event.SessionEventListener;
import com.vic.mynetty.common.factory.HeartbeatKeeperFactory;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitorFactory;
import com.vic.mynetty.common.nettycoder.codec.Codec;
import com.vic.mynetty.common.nettycoder.codec.ProtostuffCodec;
import com.vic.mynetty.common.strategyenum.BackOffStrategy;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.common.strategyenum.OpenConnStrategyEnum;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Date: 2018/9/30 16:37
 * @Description:
 */

/**
 * 客户端一些 配置，希望文件配置化
 */

@ToString
public class ClientSpecialConfig {

    // -------- connection wise --------
    /*
     * Connection open setting.
     * Default to ONE_BY_ONE since it can reduce network usage when connecting though may increase startup time.
     */
    @Getter
    private OpenConnStrategyEnum connOpenStrategy = OpenConnStrategyEnum.ONE_BY_ONE;
    /*
     * Connection reconnection setting
     */
    @Getter
    private BackOffStrategy reconnectStrategy = BackOffStrategy.RANDOM_BACKOFF;
    @Getter
    private double[] reconnectParameters = new double[] { 0, 5000 };
    // -------- session wise --------
    /*
     * Session heart beat setting.
     * Default to CLIENT_INITIATIVE since it can reduce load of server.
     * Default to 5*6 SECONDS since currently we think walle get lost after 30 secs no response.
     */
    @Getter
    private HeartbeatStrategy heartbeatStrategy = HeartbeatStrategy.CLIENT_INITIATIVE;
    @Getter
    private long idleTime = 6;
    @Getter
    private TimeUnit idleTimeUnit = TimeUnit.SECONDS;
    @Getter
    private long timeout = 12;
    @Getter
    private long initTimeout = 10;
    /*
     * Session network traffic control setting.
     * Default to EXPONENT_BACKOFF since such regulation react fast.
     * Default to max(pow(1000ms, 1.5), 60000ms).
     */
    @Getter
    private BackOffStrategy trafficRegulationStrategy = BackOffStrategy.EXPONENT_BACKOFF;
    @Getter
    private double[] trafficRegulationParameters = new double[] { 10, 2000 };
    /*
     * Session event listeners.
     */
    /**
     * 配置化的监听器,监听 sessoion (@link SessionEventListenerAdapter)状态的改变，开放给用户的
     */
    @Getter
    private List<SessionEventListener> sessionListeners;

    // todo 以前是 communicator <- session <- connect 三层的通知机制 ,因为想做成 一个communicator中多个session ，session中多个 connect
    //  todo 现在只做 session <- connect 两层通知和包装 ,所以去掉 commListeners 和相应的 propagator
//    /*
//     * Communicator listeners.
//     */
//    /**
//     * 配置化的监听器,监听 communicator （@link CommunicatorEventListenerAdapter） 状态的改变，开放给用户的
//     */
//    @Getter
//    private List<CommunicatorEventListener> commListeners;

    /*
     * Session configuration discoverer.
     */
    @Getter
    private Discoverer<SessionConfig> sessionCfgDiscoverer;
    @Getter
    private SessionIdleStateMonitorFactory sessionIdleStateMonitorFactory = new NettyClientSessionIdleStateMonitorFactory();
    @Getter
    private HeartbeatKeeperFactory heartbeatKeeperFactory = new ClientHeartbeatKeeperFactory();
    @Getter
    private ClientSessionFactory clientSessionFactory = new NettyClientSessionFactory();
    @Getter
    private ScheduledExecutorFactory scheduledExecutorFactory = new PooledScheduledExecutorFactory(3);
    // -------- communicator wise --------
    /*
     * Communicator serialization & deserialization setting.
     * Default to Protostuff since it's currently known the most efficient.
     */
    @Getter
    private Codec codec = new ProtostuffCodec();

    @Getter
    private String userId;
    @Getter
    private int netDelayMultiplier = 10;
    @Getter
    private int netDelayEventCount = 10;
    @Getter
    private long networkDelayMills = -1;
    @Getter
    private Map<String, FutureListener<?>> staticSubscriptions = new HashMap<String, FutureListener<?>>();
    public <T> ClientSpecialConfig subscribe(String path, FutureListener<T> future) {
        staticSubscriptions.put(path, future);
        return this;
    }

    // -------- connection wise --------
    public ClientSpecialConfig setConnectionOpenStrategy(OpenConnStrategyEnum connOpenStrategy) {
        this.connOpenStrategy = connOpenStrategy;
        return this;
    }

    public ClientSpecialConfig setReconnectStrategy(BackOffStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
        return this;
    }

    public ClientSpecialConfig setReconnectParameters(double[] reconnectParameters) {
        this.reconnectParameters = reconnectParameters;
        return this;
    }

    // -------- session wise --------
    public ClientSpecialConfig setHeartbeatStrategy(HeartbeatStrategy heartbeatStrategy) {
        this.heartbeatStrategy = heartbeatStrategy;
        return this;
    }

    public ClientSpecialConfig setIdleTime(long idleTime) {
        this.idleTime = idleTime;
        return this;
    }

    public ClientSpecialConfig setIdleTimeUnit(TimeUnit idleTimeUnit) {
        this.idleTimeUnit = idleTimeUnit;
        return this;
    }

    public ClientSpecialConfig setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public ClientSpecialConfig setTrafficRegulationStrategy(BackOffStrategy trafficRegulationStrategy) {
        this.trafficRegulationStrategy = trafficRegulationStrategy;
        return this;
    }

    public ClientSpecialConfig setTrafficRegulationParameters(double[] trafficRegulationParameters) {
        this.trafficRegulationParameters = trafficRegulationParameters;
        return this;
    }

    public ClientSpecialConfig addSessionListener(SessionEventListener sessionListener) {
        if (this.sessionListeners == null) {
            this.sessionListeners = new ArrayList<SessionEventListener>();
        }
        this.sessionListeners.add(sessionListener);
        return this;
    }

    public ClientSpecialConfig addSessionListeners(List<SessionEventListener> sessionListeners) {
        if (this.sessionListeners == null) {
            this.sessionListeners = new ArrayList<SessionEventListener>();
        }
        this.sessionListeners.addAll(sessionListeners);
        return this;
    }

    public ClientSpecialConfig setSessionCfgDiscoverer(Discoverer<SessionConfig> sessionCfgDiscoverer) {
        this.sessionCfgDiscoverer = sessionCfgDiscoverer;
        return this;
    }

    public ClientSpecialConfig setSessionIdleStateFactory(SessionIdleStateMonitorFactory sessionIdleStateMonitorFactory) {
        this.sessionIdleStateMonitorFactory = sessionIdleStateMonitorFactory;
        return this;
    }

    public ClientSpecialConfig setHeartbeatKeeperFactory(HeartbeatKeeperFactory heartbeatKeeperFactory) {
        this.heartbeatKeeperFactory = heartbeatKeeperFactory;
        return this;
    }

    public ClientSpecialConfig setSessionFactory(ClientSessionFactory clientSessionFactory) {
        this.clientSessionFactory = clientSessionFactory;
        return this;
    }

    public ClientSpecialConfig setScheduledExecutorFactory(ScheduledExecutorFactory scheduledExecutorFactory) {
        this.scheduledExecutorFactory = scheduledExecutorFactory;
        return this;
    }

    // -------- communicator wise --------
    public ClientSpecialConfig setCodec(Codec codec) {
        this.codec = codec;
        return this;
    }

//    public ClientSpecialConfig addCommListener(CommunicatorEventListener commListener) {
//        if (commListeners == null) {
//            commListeners = new ArrayList<CommunicatorEventListener>();
//        }
//        commListeners.add(commListener);
//        return this;
//    }

//    public ClientSpecialConfig addCommListeners(List<CommunicatorEventListener> commListeners) {
//        if (this.commListeners == null) {
//            this.commListeners = new ArrayList<CommunicatorEventListener>();
//        }
//        this.commListeners.addAll(commListeners);
//        return this;
//    }

    public ClientSpecialConfig setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public ClientSpecialConfig setNetDelayMultiplier(int netDelayMultiplier) {
        this.netDelayMultiplier = netDelayMultiplier;
        return this;
    }
    public ClientSpecialConfig setNetDelayEventCount(int netDelayEventCount) {
        this.netDelayEventCount = netDelayEventCount;
        return this;
    }
    public ClientSpecialConfig setNetworkDelayMills(long networkDelayMills) {
        this.networkDelayMills = networkDelayMills;
        return this;
    }


    public static void main(String[] args) {
        ClientSpecialConfig commConfig = new ClientSpecialConfig();
        commConfig.validate();
    }

    public void validate() {
        StringBuilder errorMsg = null;
        String header = "CommunicatorConfig is not properly set: ";
        if (this.connOpenStrategy == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("connOpenStrategy should not be null").append(",");
        }
        if (this.reconnectStrategy == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("reconnectStrategy should not be null").append(",");
        }
        if (this.reconnectParameters == null || this.reconnectParameters.length == 0) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("reconnectParameters should not be null or empty").append(",");
        }
        if (this.heartbeatStrategy == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("heartbeatStrategy should not be null").append(",");
        }
        if (this.idleTime <= 0) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("idleTime should not be lower than 0").append(",");
        }
        if (this.idleTimeUnit == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("idleTimeUnit should not be null").append(",");
        }
        if (this.timeout <= 0) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("timeout should not be lower than 0").append(",");
        }
        if (this.trafficRegulationStrategy == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("trafficRegulationStrategy should not be null").append(",");
        }
        if (this.trafficRegulationParameters == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("trafficRegulationParameters should not be null or empty").append(",");
        }
        if (this.sessionCfgDiscoverer == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("sessionCfgDiscoverer should not be null").append(",");
        }
        if (this.codec == null) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("codec should not be null").append(",");
        }
        if (this.netDelayMultiplier <= -1 && this.networkDelayMills < -1) {
            if (errorMsg == null) {
                errorMsg = new StringBuilder(header);
            }
            errorMsg.append("netDelayMultiplier & networkDelayMills should not be negative at same time").append(",");
        }
        if (errorMsg != null) {
            errorMsg.deleteCharAt(errorMsg.length() - 1);
            throw new IllegalArgumentException(errorMsg.toString());
        }
    }
}
