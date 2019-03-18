package com.vic.mynetty.netty_server.nettyserver;

import com.vic.mynetty.common.nettycoder.NettyDecoder;
import com.vic.mynetty.common.nettycoder.NettyEncoder;
import com.vic.mynetty.common.nettycoder.codec.Codec;
import com.vic.mynetty.common.nettycoder.codec.ProtostuffCodec;
import com.vic.mynetty.netty_server.ServerContext;
import com.vic.mynetty.netty_server.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Date: 2018/9/30 11:15
 * @Description:
 */
@Slf4j
public class NettyServer implements ConnectionAcceptor {

    @Setter
    private int port;
    @Setter
    private Codec codec;
    @Setter
    private ServerContext serverContext;

    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel channel;

    //test
    public static void main(String[] args) throws Exception {
        Codec codec = new ProtostuffCodec();
        ServerContext serverContext = new ServerContext();
        NettyServer server = new NettyServer(codec,8082,serverContext);
        server.start();
    }

    public void start() {
        try {
            log.debug("STARTING");
            channel = serverBootstrap.bind(port).sync().channel();
        } catch (InterruptedException e) {
            log.debug("START_FAILED", e);
        }
    }

    public NettyServer(final Codec codec,
                       int port,
                       final ServerContext serverContext){
        //todo
        this.serverContext = serverContext;
        this.serverContext.onStoresReady();
        this.port = port;
        this.codec = codec;

//        final SslContext sslCtx;

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_BACKLOG, 4096)
                .childOption(ChannelOption.SO_SNDBUF, 1048576)
                .childOption(ChannelOption.SO_RCVBUF, 1048576)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
                        //             	 ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        ch.pipeline().addLast(new NettyDecoder(codec));
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        ch.pipeline().addLast(new NettyEncoder(codec));
                        ch.pipeline().addLast(new NettyServerHandler(serverContext));
                    }
                });
    }

    public void stop() {
        channel.disconnect();
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    public void restart() {
        stop();
        start();
    }
}
