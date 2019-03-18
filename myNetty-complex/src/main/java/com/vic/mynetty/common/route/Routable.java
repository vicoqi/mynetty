package com.vic.mynetty.common.route;

public interface Routable<R extends Route, T> {
	T route(R route);
}
