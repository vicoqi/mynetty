package com.vic.mynetty.netty_server.api;


import com.vic.mynetty.netty_client.api.ReportDemo;
import com.vic.mynetty.model.Request;

public class ReportDemoImpl implements ReportDemo {

//	@Override
//	public void schedule(Request request) {
//		System.out.println(request);
//	}

	@Override
	public void retry(Request request) {
		System.out.println(request);
	}

}
