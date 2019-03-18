package com.vic.mynetty.netty_server.config;

import com.vic.mynetty.utils.URIUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@ToString
public class WorkGroupConfig {
	@Setter
    @Getter
	private String name;
	@Setter
    @Getter
	private String pathMatcher;
	@Setter
    @Getter
	private int threadCnt;
	
	public WorkGroupConfig setPathMatcher(String pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}
	
	public WorkGroupConfig setThreadCnt(int threadCnt) {
		this.threadCnt = threadCnt;
		return this;
	}
	public static class URIInterpreter {
		public static WorkGroupConfig interpret(String workGroupName, String uriStr) {
			try {
				URI workGroupURI = new URI(uriStr);
				WorkGroupConfig workGroupConfig = new WorkGroupConfig();
				Map<String, String> parsedQueryString = URIUtils.parseQueryString(workGroupURI.getQuery());
				workGroupConfig.setName(workGroupName);
				workGroupConfig.setPathMatcher(parsedQueryString.get("pathMatcher"));
				workGroupConfig.setThreadCnt(Integer.valueOf(parsedQueryString.get("threadCnt")));
				return workGroupConfig;
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(String.format("WRONG_WORK_GROUP_URISTR|current=[%s]|expected=[?pathMatcher=/path/**&threadCnt=1]", uriStr));
			}
		}
	}
}
