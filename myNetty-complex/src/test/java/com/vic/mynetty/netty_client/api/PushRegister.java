package com.vic.mynetty.netty_client.api;


import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.model.Response;

@Mapping(path="/push", type=Type.PUSH)
public interface PushRegister {
	@Mapping(path="/test")
	Future<Response> receive(String unitcast);
}
