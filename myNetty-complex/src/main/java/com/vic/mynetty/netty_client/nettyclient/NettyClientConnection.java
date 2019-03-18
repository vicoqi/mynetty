package com.vic.mynetty.netty_client.nettyclient;

import com.vic.mynetty.netty_client.config.ClientSpecialConfig;
import com.vic.mynetty.netty_client.config.ConnectionConfig;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.utils.PrintUtil;
import io.netty.channel.Channel;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyClientConnection extends AbstractClientConnection {
    @Getter
    protected Channel channel;
    private NettyClientSession nettyTCPSession;
    @Setter
    private ScheduledFuture<?> newChannelFuture;

    public NettyClientConnection(
            ClientSpecialConfig commConfig,
        String sessionName,
        String sessionId,
        ConnectionConfig connConfig,
        Object eventlock,
        NettyClientSession nettyTCPSession) {
        super(commConfig,
            sessionName,
            sessionId,
            connConfig,
            nettyTCPSession.getTrafficRegulator(),
            nettyTCPSession.getNetworkAnalyzer(),
            eventlock);
        this.nettyTCPSession = nettyTCPSession;
    }

    public void setChannel(Channel channel) {
        this.id = channel.id().asLongText();
        this.channel = channel;
    }

    public void open() {
        this.id = null;
        log.info("OPENING_CONNECTION|name=[{}]|sessnId=[{}]",
            this.name, PrintUtil.shortId(this.sessionId, PrintUtil.DELI_STRK_THRU));
        this.nettyTCPSession.newChannel(this);
    }

    @Override
    public void send(final Message message) {
        message.setNetStartTm(System.nanoTime());
        channel.writeAndFlush(message);
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

}
