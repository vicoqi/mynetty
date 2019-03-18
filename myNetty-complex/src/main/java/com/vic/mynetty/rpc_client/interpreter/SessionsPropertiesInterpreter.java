package com.vic.mynetty.rpc_client.interpreter;

import com.vic.mynetty.netty_client.config.ConnectionConfig;
import com.vic.mynetty.netty_client.config.SessionConfig;
import com.vic.mynetty.utils.PrintUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SessionsPropertiesInterpreter {

    private static final Pattern sessionURIPattern = Pattern.compile("(comm\\.)?session\\.(\\w+)\\.(uri|URI)");
    private static final Pattern connURIPattern = Pattern.compile(
        "(comm\\.)?connection\\.(\\w+)\\.(\\w+)\\.(qryStr|QRYSTR)");
    private static final String COMM_PREFIX = "comm.";

    public static List<SessionConfig> interpret(String propertiesFilePath, String root) {
        Properties properties = new Properties();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
            SessionsPropertiesInterpreter.class.getClassLoader());
        Resource[] resources;
        try {
            resources = resolver.getResources(resolver.CLASSPATH_URL_PREFIX + propertiesFilePath);
            if (resources == null || resources.length == 0) {
                throw new IllegalArgumentException(
                    String.format("property file not found in classpath|file=[%s]", propertiesFilePath));
            }
            properties.load(resources[0].getInputStream());
            return interpret(properties, root);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("property file not found in classpath|file=[%s]", propertiesFilePath), e);
        }
    }

    public static List<SessionConfig> interpret(Properties properties, String root) {
        String sessionNamesStr = properties.getProperty(root);
        String[] sessionNames = sessionNamesStr.split(",");
        List<SessionConfig> sessionConfigs = new ArrayList<SessionConfig>(sessionNames.length);
        List<ConnectionConfig> connConfigs = new ArrayList<ConnectionConfig>();
        for (Object key : properties.keySet()) {
            String keyStr = (String)key;
            String value = properties.getProperty(keyStr);
            Matcher sessionMatcher = sessionURIPattern.matcher(keyStr);
            if (sessionMatcher.matches()) {
                sessionMatcher.reset();
                if (sessionMatcher.find()) {
                    String sessionName;
                    if (sessionMatcher.groupCount() == 2) {
                        sessionName = sessionMatcher.group(1);
                    } else {
                        sessionName = sessionMatcher.group(2);
                    }
                    SessionConfig sessionConfig = SessionConfig.URIInterpreter.interpret(sessionName, value);
                    if (ArrayUtils.contains(sessionNames, sessionName)) {
                        sessionConfigs.add(sessionConfig);
                    } else {
                        log.warn("ignore wrongly configured session|name=[{}]|expectedSessions={}",
                            sessionName, PrintUtil.print(sessionNames));
                    }
                }
            }
            Matcher connMatcher = connURIPattern.matcher(keyStr);
            if (connMatcher.matches()) {
                connMatcher.reset();
                if (connMatcher.find()) {
                    String sessionName;
                    String connName;
                    if (connMatcher.groupCount() == 3) {
                        sessionName = connMatcher.group(1);
                        connName = connMatcher.group(2);
                    } else {
                        sessionName = connMatcher.group(2);
                        connName = connMatcher.group(3);
                    }
                    if (ArrayUtils.contains(sessionNames, sessionName)) {
                        ConnectionConfig connConfig = ConnectionConfig.URIInterpreter.interpret(sessionName, connName,
                            value);
                        connConfigs.add(connConfig);
                    } else {
                        log.warn("ignore wrongly configured connection|name=[{}]|session=[{}]|expectedSessions={}",
                            connName, sessionName, PrintUtil.print(sessionNames));
                    }
                }
            }
        }
        for (SessionConfig sessionConfig : sessionConfigs) {
            for (ConnectionConfig connConfig : connConfigs) {
                if (sessionConfig.getName().equals(connConfig.getSessionName())) {
                    sessionConfig.addConnectionConfig(connConfig);
                }
            }
        }
        return sessionConfigs;
    }

    public static void main(String[] args) {
        System.out.println(sessionURIPattern.matcher("a.b.URI").matches());
        System.out.println(connURIPattern.matcher("a.b.URI").matches());
        System.out.println(sessionURIPattern.matcher("a.URI").matches());
        System.out.println(connURIPattern.matcher("a.URI").matches());
        Matcher matcher = connURIPattern.matcher("a.b.URI");
        if (matcher.find()) {
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
        }
        List<SessionConfig> sessionConfigs = SessionsPropertiesInterpreter.interpret("sessions.properties",
            "comm.sessions");
        for (SessionConfig sessionConfig : sessionConfigs) {
            System.out.println(sessionConfig);
            for (ConnectionConfig connConfig : sessionConfig.getConnectionConfigs()) {
                System.out.println(connConfig);
            }
        }
    }
}
