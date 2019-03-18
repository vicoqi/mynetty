package com.vic.mynetty.common.route;

public interface RouteMatcher<R extends Route> {
	boolean matches(R route);
}
