package com.vic.mynetty.netty_server.api;


import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Mode;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.model.Request;
import com.vic.mynetty.model.Response;

@Mapping(path="/request", type=Type.REQUEST)
public interface RequestDemo {
	@Mapping(path="/sync", mode = Mode.SYNC)
	@Retry
	Response sync(Request request);
	@Mapping(path="/async", mode = Mode.ASYNC)
	@Retry
	Response async(Request request);
}
