package com.vic.mynetty.netty_client.nettyclient;

import com.vic.mynetty.netty_client.handler.NettyClientChannelHandler;
import com.vic.mynetty.common.keepalive.netty.NettyConnIdleStateHandler;
import com.vic.mynetty.common.keepalive.netty.NettySessionIdleStateMonitor;
import com.vic.mynetty.common.message.MessageFactory;
import  com.vic.mynetty.common.route.Path;
import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.ConnectionConfig;
import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.nettycoder.NettyDecoder;
import com.vic.mynetty.common.nettycoder.NettyEncoder;
import com.vic.mynetty.common.nettycoder.codec.Codec;
import com.vic.mynetty.common.strategyenum.OpenConnStrategyEnum;
import com.vic.mynetty.common.strategyenum.Protocol;
import com.vic.mynetty.utils.PrintUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 目标：1.netty通信精简，只有TCP，只有一个连接，只用客户端发心跳
 *      2.抽离 RPC 部分为独立包
 */

/**
 * @Date: 2018/9/30 11:31
 * @Description: 与服务端的连接可以看成是建立 session 会话，这个会话下面可以有很多 TCP 连接
 */
@Slf4j
public class NettyClientSession extends AbstractClientSession{

    /*
     * bootstrap related
     */
    private Bootstrap bootstrap;
    private EventLoopGroup workerGroup;

//    @Getter
//    protected ScheduledExecutorService scheduledExecutor;
    /*
     * serialization
     */
    private Codec codec;
    @Getter
    private Map<String, Message> pendingInitMessages = new HashMap<>();

    public NettyClientSession(
            ClientSpecialConfig commConfig,
            SessionConfig sessionConfig,
            Object sessionEventLock) {
        super(commConfig, sessionConfig, sessionEventLock);
        // init serialization setting
        this.codec = commConfig.getCodec();

        // prepare netty bootstrap
        bootstrap = new Bootstrap();
        workerGroup = new NioEventLoopGroup(sessionConfig.getThreadCnt());
        // if scheduledExecutor is null as a result of scheduledExecutorFactory null
        // set workerGroup as the default
        if(this.scheduledExecutor == null) {
            this.scheduledExecutor = workerGroup;
        }
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
//                	ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        ch.pipeline().addLast(new NettyDecoder(codec));
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        ch.pipeline().addLast(new NettyEncoder(codec));
                        ch.pipeline().addLast(new NettyConnIdleStateHandler((NettySessionIdleStateMonitor) sessionIdleStateMonitor));
                        ch.pipeline().addLast(new NettyClientChannelHandler(NettyClientSession.this));
                    }
                });
        // create netty connections
        //   should not null nor empty,
        // should be checked previously because it make no sence
        // that no connections inside a ssession
        List<ConnectionConfig> connConfigs = sessionConfig.getConnectionConfigs();
        connections = new ArrayList<Connection>(connConfigs.size());
        this.routeMatcher = new Path.Matcher(sessionConfig.getPathMatcher());
        this.routeMatcher.compile();
        for (ConnectionConfig connConfig : connConfigs) {
            log.info("CREATING_CONNECTION|name=[{}]|sessnNm=[{}]", connConfig.getName(), this.name);
            NettyClientConnection nettyTCPConn = new NettyClientConnection(commConfig, this.name, this.id, connConfig, this.connEventLock, this);
            nettyTCPConn.setExecutor(this.scheduledExecutor);
            nettyTCPConn.getEventfirer().addListener(this.eventPropagator);

            if (nettyTCPConn.getName().equalsIgnoreCase(Connection.DEFAULT_CONN_NAME)) {
                this.defaultConn = nettyTCPConn;
            }

            log.info("CONNECTION_CREATED|name=[{}]|sessnNm=[{}]", connConfig.getName(), this.name);
            connections.add(nettyTCPConn);
        }
    }


    public static void main(String[] args) {
        log.info("starting-------->");
        ClientSpecialConfig commConfig = new ClientSpecialConfig();
        commConfig.setConnectionOpenStrategy(OpenConnStrategyEnum.ALL_AT_ONCE);
        //		commConfig.setReconnectStrategy(BackOffStrategy.EXPONENT_BACKOFF);
        //		commConfig.setReconnectParameters(new double[]{1000, 1.5, 10000});
        commConfig.setIdleTime(5000);
        commConfig.setTimeout(2);
        SessionConfig sessionConfig = new SessionConfig("session1");
        sessionConfig.setProtocol(Protocol.TCP);
        sessionConfig.setHost("192.168.20.38");
        sessionConfig.setPort(1010);
        List<ConnectionConfig> connConfigs = new ArrayList<ConnectionConfig>();
        ConnectionConfig connConfig = new ConnectionConfig("session1", "connection1");
        connConfig.setPathMatcher("/a/**");
        connConfig.setHeartbeatConn(true);
        connConfigs.add(connConfig);
        ConnectionConfig connConfig1 = new ConnectionConfig("session1", "connection2");
        connConfig1.setPathMatcher("/b/**");
        connConfig1.setHeartbeatConn(true);
        connConfigs.add(connConfig1);
        sessionConfig.addConnectionConfigs(connConfigs);
        NettyClientSession session = new NettyClientSession(commConfig, sessionConfig, new Object());
        session.open();
    }

    public Connection query(Channel channel) {
        String channelId = channel.id().asLongText();
        for (Connection nettyTCPConn : connections) {
            String id = nettyTCPConn.getId();
            if (id != null && id.equals(channelId)) {
                return nettyTCPConn;
            }
        }
        throw new IllegalArgumentException(String.format("CONNECTION_NOT_FOUND|channlId=[%s]", channelId));
    }

    public void newChannel(final NettyClientConnection nettyTCPConn) {
        log.info("NEW_CHANNEL|name=[{}]|sessnId=[{}]",
                nettyTCPConn.getName(), PrintUtil.shortId(nettyTCPConn.getSessionId(), PrintUtil.DELI_STRK_THRU));
        // open brand new connection
        final ChannelFuture channelFuture = bootstrap.connect(host, port);

        // add listeners to trigger recreate when first open trail failed
        channelFuture.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) {
                // retry open if failed
                // reuse current EventLoop to optimize thread usage
                if (!future.isSuccess()) {
                    long backOffMills = nettyTCPConn.getReconnectBackOffCalc().calculate();
                    log.info("NEW_CHANNEL_FAILED_THEN_RETRY|name=[{}]|sessnId=[{}]|bakOffMillis=[{}]|retringTimes=[{}]",
                            nettyTCPConn.getName(), PrintUtil.shortId(nettyTCPConn.getSessionId(), PrintUtil.DELI_STRK_THRU), backOffMills, nettyTCPConn.getReconnectBackOffCalc().getTriedTimes());
                    nettyTCPConn.setNewChannelFuture(workerGroup.schedule(new Runnable() {

                        @Override
                        public void run() {
                            newChannel(nettyTCPConn);
                        }
                    }, backOffMills, TimeUnit.MILLISECONDS)); // retry after some delay to avoid net storm
                    // TODO: add back off strategy. make it configurable -> self adaptable
                } else {
                    Channel channel = future.channel();
                    nettyTCPConn.setChannel(channel);
                    Message initMessage = MessageFactory.newInitMessage(NettyClientSession.this, nettyTCPConn);
                    pendingInitMessages.put(initMessage.getId(), initMessage);
                    log.info("NEW_CHANNEL_SUCCEED_SENDING_INIT_MESSAGE|sessnId=[{}]|chnnlId=[{}]|name=[{}]|initMsg=[{}]",
                            PrintUtil.shortId(nettyTCPConn.getSessionId(), PrintUtil.DELI_STRK_THRU),PrintUtil.shortId(nettyTCPConn.getId(), PrintUtil.DELI_STRK_THRU), nettyTCPConn.getName(), initMessage);
                    channel.writeAndFlush(initMessage);
                    channel.eventLoop().schedule(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = pendingInitMessages.remove(initMessage.getId());
                            if(msg != null) {
                                log.info("NEW_CHANNEL_INIT_CHECK_FAILED|chnnlId=[{}]|name=[{}]|sessnId=[{}]",
                                        channel.id().asShortText(), nettyTCPConn.getName(), PrintUtil.shortId(nettyTCPConn.getSessionId(), PrintUtil.DELI_STRK_THRU));
                                channel.close();
                            } else {
                                log.info("NEW_CHANNEL_INIT_CHECK_PASS|chnnlId=[{}]|name=[{}]|sessnId=[{}]",
                                        channel.id().asShortText(), nettyTCPConn.getName(), PrintUtil.shortId(nettyTCPConn.getSessionId(), PrintUtil.DELI_STRK_THRU));
                            }
                        }}, nettyTCPConn.getInitTimeout(), TimeUnit.SECONDS);
                }
            }
        });
    }
}
