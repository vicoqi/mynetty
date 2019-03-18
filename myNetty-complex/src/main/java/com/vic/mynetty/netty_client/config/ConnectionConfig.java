package com.vic.mynetty.netty_client.config;

import com.vic.mynetty.common.Connection;
import com.vic.mynetty.utils.URIUtils;
import lombok.*;

import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@ToString
public class ConnectionConfig {
	/*
	 * property key
	 */
	@NonNull
	@Getter
	private String sessionName;
	@NonNull
	@Getter
	private String name;
	/*
	 * interpreted from property value
	 */
	@Setter
    @Getter
	private String pathMatcher;
	@Setter
    @Getter
	private boolean heartbeatConn;
	
	public static class URIInterpreter {
		public static ConnectionConfig interpret(String sessionName, String connName, String uriStr) {
			try {
				URI connURI = new URI(uriStr);
				ConnectionConfig connectionConfig = new ConnectionConfig(sessionName, connName);
				Map<String, String> parsedQueryString = URIUtils.parseQueryString(connURI.getQuery());
				if (!connName.equalsIgnoreCase(Connection.DEFAULT_CONN_NAME)
						&& parsedQueryString.containsKey("pathMatcher")) {
					connectionConfig.setPathMatcher(parsedQueryString.get("pathMatcher"));
				}
				if (parsedQueryString.containsKey("heartbeatConn")) {
					connectionConfig.setHeartbeatConn(Boolean.valueOf(parsedQueryString.get("heartbeatConn")));
				}
				return connectionConfig;
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("WRONG_CONNECTION_URISTR|uriStr=[%s]|expectedFormat=[/{pathMatcher}?heartbeatConn={true/false}]", uriStr), e);
			}
		}
	}
	public static void main(String[] args) throws Exception {
		// expected
		System.out.println(URIInterpreter.interpret("session1", "connection1", "/a/**?heartbeatConn=false"));
		// wrong
		System.out.println(URIInterpreter.interpret("session1", "connection1", "123"));
	}
}
