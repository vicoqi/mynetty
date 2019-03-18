package com.vic.mynetty.netty_client.api;


import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Mode;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.future.Future;
import com.vic.mynetty.model.Request;
import com.vic.mynetty.model.Response;

@Mapping(path="/request", type=Type.REQUEST)
@Retry
public interface RequestDemo {
	@Mapping(path="/sync", mode = Mode.SYNC)
//	@Retry(times=10, timeout=1000)
	Response sync(Request request);
	@Mapping(path="/async", mode = Mode.ASYNC)
//	@Retry(timeout=1000)
	Future<Response> async(Request request);
}
