package com.vic.mynetty.common.route;

public interface RouteMatchable<T extends Route> {
	RouteMatcher<T> getRouteMatcher();
}

