package com.vic.mynetty.common.keepalive;

import com.vic.mynetty.common.Session;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.message.MessageFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 被动心跳，接受心跳
 */
@Slf4j
public class PassiveHeartbeatKeeper extends HeartbeatKeeper {

    private Session session;

    public PassiveHeartbeatKeeper(Session session) {
        this.session = session;
    }

    @Override
    public void onSessionIdle(Session session, Message message) {
        confirmHeartbeat(message);
    }

    private void confirmHeartbeat(Message heartbeat) {
        Message respHeartbeart = MessageFactory.newRespHeartbeatMessage(heartbeat);
        log.info("COMFIRMING_HEART_BEAT|inMsgId=[{}]|outMsgId=[{}]",
            heartbeat.getId(), respHeartbeart.getId());
        session.getHeartbeatConn().send(respHeartbeart);
    }

}
