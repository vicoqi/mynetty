package com.vic.mynetty.common.message;


import com.vic.mynetty.netty_client.nettyclient.NettyClientConnection;
import com.vic.mynetty.netty_client.nettyclient.NettyClientSession;

public class MessageFactory {
	public static Message newInitMessage(
			NettyClientSession session,
			NettyClientConnection connection) {
		Message initMessage = new Message();
		ConnInitInfo connInitInfo = new ConnInitInfo(session, connection);
		initMessage.setMessageType(MessageType.INIT);
		initMessage.setObjects(new Object[]{connInitInfo});
		return initMessage;
	}
	
	public static Message newRespInitMessage(Message incomming) {
		return newRespMessage(incomming, null);
	}
	
	public static Message newRespMessage(Message reqMessage, Object[] result) {
		Message respMsg = new Message();
        respMsg.setCollaborationId(reqMessage.getId());
        respMsg.setMessageType(reqMessage.getMessageType());
        respMsg.setNetStartTm(reqMessage.getNetStartTm());
        respMsg.setObjects(result);
        respMsg.setMapping(reqMessage.getMapping());
        return respMsg;
	}
	
	public static Message newHeartbeatMessage() {
		Message heartbeatMsg = new Message();
		heartbeatMsg.setMessageType(MessageType.HEART_BEAT);
		return heartbeatMsg;
	}
	
	public static Message newRespHeartbeatMessage(Message incoming) {
		return newRespMessage(incoming, null);
	}

}
