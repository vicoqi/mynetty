package com.vic.mynetty.common.keepalive.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class NettyConnIdleStateHandler extends ChannelDuplexHandler {
	private NettySessionIdleStateMonitor idleStateMonitor;
	public NettyConnIdleStateHandler(NettySessionIdleStateMonitor idleStateMonitor) {
		this.idleStateMonitor = idleStateMonitor;
	}
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (idleStateMonitor.getReaderIdleTimeNanos() > 0 
        		|| idleStateMonitor.getAllIdleTimeNanos() > 0) {
        	idleStateMonitor.setReading(ctx.name(), true);
        	idleStateMonitor.setFirstReaderIdleEvent(true);
        	idleStateMonitor.setFirstAllIdleEvent(true);
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if ((idleStateMonitor.getReaderIdleTimeNanos() > 0 
        		|| idleStateMonitor.getAllIdleTimeNanos() > 0) 
        		&& idleStateMonitor.isReading(ctx.name())) {
        	idleStateMonitor.setLastReadTime(System.nanoTime());
        	idleStateMonitor.setReading(ctx.name(), false);
        }
        ctx.fireChannelReadComplete();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Allow writing with void promise if handler is only configured for read timeout events.
        if (idleStateMonitor.getWriterIdleTimeNanos() > 0 
        		|| idleStateMonitor.getAllIdleTimeNanos() > 0) {
            ChannelPromise unvoid = promise.unvoid();
            unvoid.addListener(idleStateMonitor.getWriteListener());
            ctx.write(msg, unvoid);
        } else {
            ctx.write(msg, promise);
        }
    }
}

