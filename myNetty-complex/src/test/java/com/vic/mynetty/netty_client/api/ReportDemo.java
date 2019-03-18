package com.vic.mynetty.netty_client.api;


import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Mode;
import com.vic.mynetty.common.declarative.Retry;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.model.Request;

@Mapping(path="/report", type=Type.REPORT)
public interface ReportDemo {
//	@Mapping(path="/schedule", mode = Mode.SYNC)
//	@Schedule(period=1000)
//	void schedule(Request request);
	
	@Mapping(path="/retry", mode = Mode.SYNC)
	@Retry(timeout=1000)
	void retry(Request request);
}
