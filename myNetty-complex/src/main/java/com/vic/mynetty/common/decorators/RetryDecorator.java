package com.vic.mynetty.common.decorators;


import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.common.declarative.Failure;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.qoos.BackOffCalculator;
import com.vic.mynetty.common.qoos.TrafficRegulator;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.utils.PrintUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 重试机制
 */
@Slf4j
public class RetryDecorator {

    private AbstractClientSession session;
    private TrafficRegulator trafficRegulator;
    @Setter
    private ScheduledExecutorService scheduledExecutor;

    public RetryDecorator(
            AbstractClientSession session,
            TrafficRegulator trafficRegulator,
            ScheduledExecutorService scheduledExecutor) {
        this.session = session;
        this.trafficRegulator = trafficRegulator;
        this.scheduledExecutor = scheduledExecutor;
    }

    private boolean shouldRetry(Retry.Model retry, Exception exception, long triedTimes) {
        Class<?>[] exceptions = retry.getExceptions();
        boolean matchRetryExp = false;
        if (exception != null) {
            if (exception instanceof TimeoutException) {
                if (retry.contains(Failure.TIMEOUT)) {
                    matchRetryExp = true;
                }
            } else {
                for (Class<?> item : exceptions) {
                    // 父类isAssignableFrom子类
                    if (item.isAssignableFrom(exception.getClass())) {
                        matchRetryExp = true;
                    }
                }
            }
        }
        long times = retry.getTimes();
        return matchRetryExp && (triedTimes < times || times == -1);
    }

    private boolean shouldRetry(Retry.Model retry, FutureEvent event, Exception exception, long triedTimes) {
        boolean matchRetryExp = false;
        if (retry.contains(Failure.TIMEOUT) && event == FutureEvent.TIMEOUT) {
            matchRetryExp = true;
        }
        if (retry.contains(Failure.EXCEPTION) && event == FutureEvent.FAILURE) {
            Class<?>[] exceptions = retry.getExceptions();
            if (exception != null) {
                if (exception instanceof TimeoutException) {
                    matchRetryExp = true;
                } else {
                    for (Class<?> item : exceptions) {
                        // 父类isAssignableFrom子类
                        if (item.isAssignableFrom(exception.getClass())) {
                            matchRetryExp = true;
                        }
                    }
                }
            }
        }
        long times = retry.getTimes();
        return matchRetryExp && (triedTimes < times || times == -1);
    }

    public Message syncSend4Result(Message message, Retry.Model retry) throws Exception {
        log.info("SYNC_SEND_4_RESULT_WITH_RETRY|msgId=[{}]|retry=[{}]", message.getId(), retry);
        Message ret = null;
        Exception exception;
        BackOffCalculator backOffCalc = new BackOffCalculator(
            retry.getBackoffStrategy(),
            retry.getRetryParameters(),
            trafficRegulator);
        do {
            try {
                exception = null;
                long backOffMills = backOffCalc.calculate();
                if (backOffMills != 0) {
                    log.debug("WAIT_4_NEXT_RETRY|msgId=[{}]|waitTime=[{}ms]|retryTimes=[{}]",
                        PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU), backOffMills,
                        (backOffCalc.getTriedTimes() - 1));
                    Thread.sleep(backOffMills);
                }
                ret = session.syncSend4Result(message);
            } catch (Exception e) {
                log.warn(String.format("EXCEPTION_ACCURED|msgId=[%s]",
                    PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU)), e);
                exception = e;
            }
        } while (shouldRetry(retry, exception, backOffCalc.getTriedTimes()));
        if (exception != null) {
            throw exception;
        }
        return ret;
    }

    public Future<Message> asyncSend4Result(final Message message, final Retry.Model retry) {
        log.info("ASYNC_SEND_4_RESULT_WITH_RETRY|msgId=[{}]|retry=[{}]", message.getId(), retry);
        final Future<Message> future = new Future<Message>();
//        session.asyncSend4Result(message,future);
        final BackOffCalculator backOffCalc = new BackOffCalculator(
            retry.getBackoffStrategy(),
            retry.getRetryParameters(),
            trafficRegulator);
        final Future<Message> futureResult = new Future<Message>();
        futureResult.setRunnable(new Runnable() {

            @Override
            public void run() {
                final Runnable task = new Runnable() {

                    @Override
                    public void run() {
//                        future.begin();
                        session.asyncSend4Result(message,future);
                    }
                };
                future.addListener(new FutureListener<Message>() {

                    public void onEvent(FutureEvent event, Message b, Exception e) {
                        switch (event) {
                            case SUCCESS:
                                futureResult.fireEvent(FutureEvent.SUCCESS, b, e);
                                break;
                            case FAILURE:
                            case TIMEOUT:
                                log.warn(String.format("EXCEPTION_ACCURED|msgId=[%s]",
                                    PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU)), e);
                                if (shouldRetry(retry, event, e, backOffCalc.getTriedTimes())) {
                                    long backOffMills = backOffCalc.calculate();
                                    log.debug("WAIT_4_NEXT_RETRY|msgId=[{}]|waitTime=[{}ms]|retryTimes=[{}]",
                                        PrintUtil.shortId(message.getId(), PrintUtil.DELI_STRK_THRU), backOffMills,
                                        (backOffCalc.getTriedTimes() - 1));
                                    scheduledExecutor.schedule(task, backOffMills, TimeUnit.MILLISECONDS);
                                } else {
                                    futureResult.fireEvent(event, b, e);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
                task.run();
            }
        });
        return futureResult;
    }

    public void syncSend(Message message, Retry.Model retry) throws Exception {
        Exception exception;
        BackOffCalculator backOffCalc = new BackOffCalculator(
            retry.getBackoffStrategy(),
            retry.getRetryParameters(),
            trafficRegulator);
        do {
            try {
                exception = null;
                Thread.sleep(backOffCalc.calculate());
                session.send(message);
            } catch (Exception e) {
                exception = e;
            }
        } while (shouldRetry(retry, exception, backOffCalc.getTriedTimes()));
        //todo 这里有点儿问题，如果重试过了，那么异常还是会抛出的，应该抛出新的异常类（这个异常类可以定义为通信异常类），在外面捕获
        if (exception != null) {
            throw exception;
        }
    }

    public void asyncSend(final Message message, final Retry.Model retry) {
        final BackOffCalculator backOffCalc = new BackOffCalculator(
            retry.getBackoffStrategy(),
            retry.getRetryParameters(),
            trafficRegulator);
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    //现在这个睡眠时间写的是根据网络情况，随机时间
                    Thread.sleep(backOffCalc.calculate());
                    session.send(message);
                } catch (Exception e) {
                    log.error("异步发送失败",e);
                    if (shouldRetry(retry, e, backOffCalc.getTriedTimes())) {
                        scheduledExecutor.schedule(this, backOffCalc.calculate(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        };
        scheduledExecutor.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

}
