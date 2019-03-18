package com.vic.mynetty.common.message;

import com.vic.mynetty.common.declarative.Mapping;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
public class Message {
	@Getter
	private String id;
	@Setter
    @Getter
	private MessageType messageType;
	@Setter
    @Getter
	private Mapping.Model mapping;
	@Setter
    @Getter
	private Object[] objects;
	@Setter
    @Getter
	private long respTimeoutMillis;
	@Setter
    @Getter
	private String collaborationId;
	@Setter
    @Getter
	private String errorMsg;
	@Getter
    @Setter
	private long netStartTm;
	@Getter
    @Setter
	private long netEndTm;
	public Message() {
		this.id = UUID.randomUUID().toString();
	}
}

