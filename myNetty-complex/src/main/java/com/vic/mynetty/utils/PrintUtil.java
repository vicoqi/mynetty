package com.vic.mynetty.utils;

import com.vic.mynetty.common.route.Path;
import com.vic.mynetty.common.route.Route;
import com.vic.mynetty.common.route.RouteMatchable;
import com.vic.mynetty.common.route.RouteMatcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class PrintUtil {
	private static String LINE_SEPARATOR;
	public static final String DELI_STRK_THRU = "-";
	
	static {
		LINE_SEPARATOR = System.getProperty("line.separator");
	}
	
	public static <T> String printList(List<T> list) {
		StringBuilder sb = new StringBuilder();
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i));
			if (i != (list.size() - 1)) {
				sb.append(LINE_SEPARATOR);
			}
		}
		return sb.toString();
	}
	
	public static String print(double[] doubleArr) {
		if (doubleArr == null) {
			return null;
		}
		if (doubleArr.length == 0) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (double d : doubleArr) {
			sb.append(d).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		return sb.toString();
	}
	
	public static String print(Object[] doubleArr) {
		if (doubleArr == null) {
			return null;
		}
		if (doubleArr.length == 0) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Object d : doubleArr) {
			sb.append(d).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		return sb.toString();
	}
	
	public static <T extends Route> String printRouteMatchers(List<? extends RouteMatchable<T>> routeMatchables) {
		StringBuffer sb = new StringBuffer("");
		for (RouteMatchable<T> routeMatchable : routeMatchables) {
			sb.append(routeMatchable.getRouteMatcher()).append("|");
		}
		if (sb.length() > 1) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}
	
	public static String shortId(String longId, String delimeter) {
		if (StringUtils.isEmpty(longId)) {
			return "";
		}
		return longId.substring(longId.lastIndexOf(delimeter) + 1);
	}
	
	public static void main(String[] args) {
//		System.out.println(PrintUtil.print(new double[]{1,2,3,4}));
//		List<RouteMatchable<Path>> routeMatchables = new ArrayList<>();
//		routeMatchables.add(new RM());
//		routeMatchables.add(new RM());
//		routeMatchables.add(new RM());
//		System.out.println(printRouteMatchers(routeMatchables));
//		List<Integer> il = new ArrayList<Integer>();
//		il.add(1);
//		il.add(2);
//		System.out.println(printList(il));
		System.out.println(shortId("a", "-"));
	}
	
	static class RM implements RouteMatchable<Path> {

		@Override
		public RouteMatcher<Path> getRouteMatcher() {
			return new Path.Matcher("/a/**");
		}
		
	}
}
