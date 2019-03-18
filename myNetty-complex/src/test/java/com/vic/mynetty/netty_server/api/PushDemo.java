package com.vic.mynetty.netty_server.api;

import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.model.Response;
import com.vic.mynetty.rpc_common.service.Publisher;

@Mapping(path="/push", type=Type.PUSH)
public interface PushDemo {
	@Mapping(path="/test")
	Publisher push(Response response);
}
