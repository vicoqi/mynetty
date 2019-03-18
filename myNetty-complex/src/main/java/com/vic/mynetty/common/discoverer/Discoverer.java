package com.vic.mynetty.common.discoverer;

import java.util.List;

public interface Discoverer<T> {
	List<T> discover();
}
