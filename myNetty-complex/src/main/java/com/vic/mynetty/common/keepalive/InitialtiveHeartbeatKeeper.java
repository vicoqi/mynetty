package com.vic.mynetty.common.keepalive;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.future.TimingFutureListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.message.MessageFactory;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 主动心跳，发起心跳
 */
@Slf4j
public class InitialtiveHeartbeatKeeper extends HeartbeatKeeper {

    private List<Message> unconfirmedHeartbeats;
    private Session session;
    private long idleTime;
    private TimeUnit idleTimeUnit;
    private long idleTimeMills;
    //超时次数
    private long timeout;

    public InitialtiveHeartbeatKeeper(Session session) {
        this.session = session;
        this.idleTime = session.getAllIdleTime();
        this.idleTimeUnit = session.getIdleTimeUnit();
        this.idleTimeMills = session.getIdleTimeUnit().toMillis(idleTime);
        this.timeout = session.getTimeout();
    }

    @Override
    public void onSessionReady(Session session) {
        unconfirmedHeartbeats = null;
    }

    //每次读写 空闲会触发这里，如果 unconfirmedHeartbeats 超过了 timeout ，出发sesssion lost
    @Override
    public void onSessionIdle(Session session, Message message) {
        if (unconfirmedHeartbeats != null && unconfirmedHeartbeats.size() > timeout) {
            session.getEventfirer().fireSessionLost();
            return;
        }
        fireHeartbeat();
    }

    private void fireHeartbeat() {
        final Message heartbeat = MessageFactory.newHeartbeatMessage();
        final Future<Message> future = new Future<Message>();
        future.setRunnable(new Runnable() {
            public void run() {
                try {
                    Connection conn = session.getHeartbeatConn();
                    final long expireTm = System.currentTimeMillis() + InitialtiveHeartbeatKeeper.this.idleTimeMills;
                    TimingFutureListener<Message> heartbeatFuture = new TimingFutureListener<Message>() {
                        @Override
                        public void onEvent(FutureEvent event, Message t, Exception e) {
                            future.fireEvent(FutureEvent.SUCCESS, t, e);
                        }
                    };
                    heartbeatFuture.setExpireTm(expireTm);
                    conn.bind(heartbeat, heartbeatFuture);
                    if (unconfirmedHeartbeats == null) {
                        unconfirmedHeartbeats = new ArrayList<Message>((int)InitialtiveHeartbeatKeeper.this.timeout);
                    }
                    unconfirmedHeartbeats.add(heartbeat);
                    log.info("SENDING_HEART_BEAT|msgId=[{}]", heartbeat.getId());
                    conn.send(heartbeat);
                } catch (Exception e) {
                    future.fireEvent(FutureEvent.FAILURE, null, e);
                }
            }
        }).addListener(new FutureListener<Message>() {

            @Override
            public synchronized void onEvent(FutureEvent event, Message t, Exception e) {
                if (CollectionUtils.isEmpty(unconfirmedHeartbeats)) {
                    return;
                }
                int indexFound = -1;
                for (int i = 0; i < unconfirmedHeartbeats.size(); i++) {
                    if (t != null && unconfirmedHeartbeats.get(i).getId().equals(t.getCollaborationId())) {
                        indexFound = i;
                        break;
                    }
                }
                if (indexFound == -1) {
                    return;
                }
                int leftSize = unconfirmedHeartbeats.size() - indexFound - 1;
                if (leftSize == 0) {
                    unconfirmedHeartbeats = null;
                } else {
                    List<Message> leftUnconfirmed = new ArrayList<Message>(leftSize);
                    for (int j = (indexFound + 1); j < unconfirmedHeartbeats.size(); j++) {
                        leftUnconfirmed.add(unconfirmedHeartbeats.get(j));
                    }
                    unconfirmedHeartbeats = leftUnconfirmed;
                }
            }
        }).begin();
    }

}
