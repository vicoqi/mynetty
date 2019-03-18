package com.vic.mynetty.common.keepalive.netty;


import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.common.AbstractSession;
import com.vic.mynetty.common.event.SessionEventFirer;
import com.vic.mynetty.common.keepalive.SessionIdleStateMonitor;
import com.vic.mynetty.common.state.SessionState;
import com.vic.mynetty.common.strategyenum.HeartbeatStrategy;
import com.vic.mynetty.netty_server.session.NettyServerSession;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NettySessionIdleStateMonitor implements SessionIdleStateMonitor {
	private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    // Not create a new ChannelFutureListener per write operation to reduce GC pressure.
	@Getter
    private final ChannelFutureListener writeListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            lastWriteTime = System.nanoTime();
            firstWriterIdleEvent = firstAllIdleEvent = true;
        }
    };
    
    @Getter
    private final long readerIdleTimeNanos;
    @Getter
    private final long writerIdleTimeNanos;
    @Getter
    private final long allIdleTimeNanos;
    @Getter
    private ScheduledFuture<?> readerIdleTimeout;
    @Getter
    @Setter
    private long lastReadTime;
    @Getter
    @Setter
    private boolean firstReaderIdleEvent = true;
    @Getter
    private ScheduledFuture<?> writerIdleTimeout;
    @Getter
    private long lastWriteTime;
    @Getter
    @Setter
    private boolean firstWriterIdleEvent = true;
    @Getter
    private ScheduledFuture<?> allIdleTimeout;
    @Getter
    @Setter
    private boolean firstAllIdleEvent = true;
    @Getter
    private AtomicInteger state = new AtomicInteger(0); // 0 - none, 1 - initialized, 2 - destroyed
    private Map<String, Boolean> readingStates = new ConcurrentHashMap<String, Boolean>();
    private ScheduledExecutorService executor;
    private SessionEventFirer eventfirer;
    private AbstractSession session;
    private boolean idleMonitor;
    public void setReading(String ctxName, boolean reading) {
    	readingStates.put(ctxName, reading);
    }
    public boolean isReading(String ctxName) {
    	Boolean isReading = readingStates.get(ctxName);
    	return isReading != null && isReading.booleanValue() == true ? true : false;
    }
    public boolean isReading() {
    	if (readingStates.containsValue(true)) {
			return true;
		}
    	return false;
    }
    
    public NettySessionIdleStateMonitor(
    		AbstractSession session,
    		long allIdleTime, 
    		TimeUnit unit) {
    	this(allIdleTime, unit);
    	this.session = session;
    	this.executor = session.getScheduledExecutor();
    	this.eventfirer = session.getEventfirer();
    	if ((session instanceof AbstractClientSession
    			&& session.getHeartbeatStrategy() == HeartbeatStrategy.CLIENT_INITIATIVE)
    		||
			(session instanceof NettyServerSession
	    			&& session.getHeartbeatStrategy() == HeartbeatStrategy.SERVER_INITIATIVE)) {
			idleMonitor = true;
		} else {
			idleMonitor = false;
		}
    }
    
    public NettySessionIdleStateMonitor(
            int readerIdleTimeSeconds,
            int writerIdleTimeSeconds,
            int allIdleTimeSeconds) {
        this(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds,
             TimeUnit.SECONDS);
    }
    
    public NettySessionIdleStateMonitor(
    		long readIdleTime, 
    		TimeUnit unit) {
    	this(readIdleTime, 0, 0, unit);
    }
    
    public NettySessionIdleStateMonitor(
            long readerIdleTime, long writerIdleTime, long allIdleTime,
            TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (writerIdleTime <= 0) {
            writerIdleTimeNanos = 0;
        } else {
            writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (allIdleTime <= 0) {
            allIdleTimeNanos = 0;
        } else {
            allIdleTimeNanos = Math.max(unit.toNanos(allIdleTime), MIN_TIMEOUT_NANOS);
        }
    }
    
    public long getReaderIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(readerIdleTimeNanos);
    }
    
    public long getWriterIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(writerIdleTimeNanos);
    }
    
    public long getAllIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(allIdleTimeNanos);
    }
    
    public void initialize() {
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
    	if (state.compareAndSet(0, 1)) {
    		lastReadTime = lastWriteTime = System.nanoTime();
    		if (readerIdleTimeNanos > 0) {
    			readerIdleTimeout = executor.schedule(
    					new ReaderIdleTimeoutTask(),
    					readerIdleTimeNanos, TimeUnit.NANOSECONDS);
    		}
    		if (writerIdleTimeNanos > 0) {
    			writerIdleTimeout = executor.schedule(
    					new WriterIdleTimeoutTask(),
    					writerIdleTimeNanos, TimeUnit.NANOSECONDS);
    		}
    		if (allIdleTimeNanos > 0) {
    			allIdleTimeout = executor.schedule(
    					new AllIdleTimeoutTask(),
    					allIdleTimeNanos, TimeUnit.NANOSECONDS);
    		}
		}
    }

    public void destroy() {
    	if (state.compareAndSet(1, 0)) {
    		if (readerIdleTimeout != null) {
    			readerIdleTimeout.cancel(false);
    			readerIdleTimeout = null;
    		}
    		if (writerIdleTimeout != null) {
    			writerIdleTimeout.cancel(false);
    			writerIdleTimeout = null;
    		}
    		if (allIdleTimeout != null) {
    			allIdleTimeout.cancel(false);
    			allIdleTimeout = null;
    		}
    		state.set(0);
		}
    }
    
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }
    
    protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
        switch (state) {
            case ALL_IDLE:
                return first ? IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT : IdleStateEvent.ALL_IDLE_STATE_EVENT;
            case READER_IDLE:
                return first ? IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT : IdleStateEvent.READER_IDLE_STATE_EVENT;
            case WRITER_IDLE:
                return first ? IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT : IdleStateEvent.WRITER_IDLE_STATE_EVENT;
            default:
                throw new Error();
        }
    }

    private final class ReaderIdleTimeoutTask implements Runnable {

        ReaderIdleTimeoutTask() {
        }

        @Override
        public void run() {
            if (session.getState() != SessionState.READY) {
                return;
            }

            long nextDelay = readerIdleTimeNanos;
            if (!isReading()) {
                nextDelay -= System.nanoTime() - lastReadTime;
            }

            if (nextDelay <= 0) {
                // Reader is idle - set a new timeout and notify the callback.
                readerIdleTimeout =
                		executor.schedule(this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
                if(idleMonitor) {
                	eventfirer.fireSessionIdle(null);
                } else {
                	session.setHeartTimeOut(true);
                	eventfirer.fireSessionLost();
                }
            } else {
                // Read occurred before the timeout - set a new timeout with shorter delay.
                readerIdleTimeout = executor.schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class WriterIdleTimeoutTask implements Runnable {

        WriterIdleTimeoutTask() {
        }

        @Override
        public void run() {
            if (session.getState() != SessionState.READY) {
                return;
            }

            long lastWriteTime = NettySessionIdleStateMonitor.this.lastWriteTime;
            long nextDelay = writerIdleTimeNanos - (System.nanoTime() - lastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout = executor.schedule(
                        this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
                if(idleMonitor) {
                	eventfirer.fireSessionIdle(null);
                } else {
                	eventfirer.fireSessionLost();
                }
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout = executor.schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask implements Runnable {

        @Override
        public void run() {
            if (session.getState() != SessionState.READY) {
                return;
            }

            long nextDelay = allIdleTimeNanos;
            if (!isReading()) {
                nextDelay -= System.nanoTime() - Math.max(lastReadTime, lastWriteTime);
            }
            if (nextDelay <= 0) {
                // Both reader and writer are idle - set a new timeout and
                // notify the callback.
                allIdleTimeout = executor.schedule(
                        this, allIdleTimeNanos, TimeUnit.NANOSECONDS);
                if(idleMonitor) {
                	eventfirer.fireSessionIdle(null);
                } else {
                	eventfirer.fireSessionLost();
                }
            } else {
                // Either read or write occurred before the timeout - set a new
                // timeout with shorter delay.
                allIdleTimeout = executor.schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

}
