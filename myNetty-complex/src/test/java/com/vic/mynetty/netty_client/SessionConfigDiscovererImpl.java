package com.vic.mynetty.netty_client;


import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.common.discoverer.Discoverer;
import com.vic.mynetty.rpc_client.interpreter.SessionsPropertiesInterpreter;

import java.util.List;

public class SessionConfigDiscovererImpl implements Discoverer<SessionConfig> {

	@Override
	public List<SessionConfig> discover() {
		return SessionsPropertiesInterpreter.interpret("sessions.properties", "sessions");
	}

}
