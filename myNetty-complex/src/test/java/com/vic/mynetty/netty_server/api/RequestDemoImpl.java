package com.vic.mynetty.netty_server.api;


import com.vic.mynetty.model.Request;
import com.vic.mynetty.model.Response;

public class RequestDemoImpl implements RequestDemo {

	@Override
	public Response sync(Request request) {
		System.out.println(request);
		return new Response("response", 1);
	}

	@Override
	public Response async(Request request) {
		System.out.println(request);
		return new Response("response", 2);
	}

}
