package com.vic.mynetty.netty_client.config;

import com.vic.mynetty.common.strategyenum.Protocol;
import com.vic.mynetty.utils.URIUtils;
import lombok.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Date: 2018/9/30 15:40
 * @Description:
 */

@RequiredArgsConstructor
@ToString
public class SessionConfig {
    /*
     * property key
     */
    @NonNull
    @Getter
    private String name;
    /*
     * interpreted from property value
     */
    @Setter
    @Getter
    private Protocol protocol;
    @Setter @Getter
    private String host;
    @Setter @Getter
    private int port;
    @Setter @Getter
    private int threadCnt = 1;
    @Setter @Getter
    private boolean ssl = false;
    @Setter @Getter
    private String pathMatcher;
    /*
     * set when they are ready
     */
    @Getter
    private List<ConnectionConfig> connectionConfigs;

    public void addConnectionConfig(ConnectionConfig connectionConfig) {
        if (connectionConfigs == null) {
            connectionConfigs = new ArrayList<ConnectionConfig>();
        }
        connectionConfigs.add(connectionConfig);
    }
    public void addConnectionConfigs(List<ConnectionConfig> connectionConfigs) {
        if (this.connectionConfigs == null) {
            this.connectionConfigs = new ArrayList<ConnectionConfig>();
        }
        this.connectionConfigs.addAll(connectionConfigs);
    }
    /**
     * Interpret string formated session config(protocol://host:port?ssl={true/false}) into model.
     * @author Donglongxiang
     *
     */
    public static class URIInterpreter {
        public static SessionConfig interpret(String sessionName, String uriStr) {
            try {
                URI sessionURI = new URI(uriStr);
                SessionConfig sessionConfig = new SessionConfig(sessionName);
                sessionConfig.setProtocol(Protocol.valueOf(sessionURI.getScheme().toUpperCase()));
                sessionConfig.setHost(sessionURI.getHost());
                sessionConfig.setPort(sessionURI.getPort());
                Map<String, String> parsedQueryString = URIUtils.parseQueryString(sessionURI.getQuery());
                if (parsedQueryString.containsKey("pathMatcher")) {
                    sessionConfig.setPathMatcher(parsedQueryString.get("pathMatcher"));
                }
                if (parsedQueryString.containsKey("ssl")) {
                    sessionConfig.setSsl(Boolean.valueOf(parsedQueryString.get("ssl")));
                }
                if (parsedQueryString.containsKey("threadCnt")) {
                    sessionConfig.setThreadCnt(Integer.valueOf(parsedQueryString.get("threadCnt")));
                }
                return sessionConfig;
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("WRONG_SESSION_URISTR|uriStr=[%s]|expected=[protocol://host:port?ssl={true/false}]", uriStr));
            }
        }
    }
    public static void main(String[] args) {
        // expected
        System.out.println(SessionConfig.URIInterpreter.interpret("session1", "tcp://127.0.0.1:1010?ssl=true"));
        // wrong
        System.out.println(SessionConfig.URIInterpreter.interpret("session1", "abc://127.0.0.1:1010?ssl=true"));
    }
}
