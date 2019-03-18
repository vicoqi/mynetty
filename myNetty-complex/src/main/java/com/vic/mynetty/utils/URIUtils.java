package com.vic.mynetty.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class URIUtils {
	public static Map<String, String> parseQueryString(String queryStr) {
		Map<String, String> parsedKVs = new HashMap<String, String>();
		if (StringUtils.isEmpty(queryStr)) {
			return parsedKVs;
		}
		String[] kvs = queryStr.split("&");
		for (String kv : kvs) {
			String[] kvArr = kv.split("=");
			if (kvArr.length == 1) {
				parsedKVs.put(kvArr[0], "");
			} else {
				parsedKVs.put(kvArr[0], kvArr[1]);
			}
		}
		return parsedKVs;
	}
}
