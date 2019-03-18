package com.vic.mynetty.rpc_common.message;

import com.vic.mynetty.netty_client.Communication;
import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.message.MessageType;

public class MessageFactory {

	public static Message newMessage(Communication comm) {
		Message message = new Message();
		message.setMessageType(MessageType.DATA);
		message.setMapping(comm.getMapping());
		message.setObjects(comm.getParameters());
		if (comm.getRetry() == null) {
			message.setRespTimeoutMillis(Retry.DEFAULT_TIMEOUT_MILLS);
		} else {
			message.setRespTimeoutMillis(comm.getRetry().getTimeout());
		}
		return message;
	}
	
	public static Message newRegisterMessage(Communication communication) {
		Message message = new Message();
		message.setMessageType(MessageType.DATA);
		message.setMapping(communication.getMapping());
		message.setObjects(communication.getParameters());
		message.setRespTimeoutMillis(-1);
		return message;
	}
	
	public static Message newPushMessage(Mapping.Model mapping, Object[] args) {
		Message message = new Message();
		message.setMessageType(MessageType.DATA);
		message.setObjects(args);
		message.setMapping(mapping);
		return message;
	}
	
}
