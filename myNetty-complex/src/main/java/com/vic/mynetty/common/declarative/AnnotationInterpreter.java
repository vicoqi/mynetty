package com.vic.mynetty.common.declarative;

import java.lang.reflect.Method;

public interface AnnotationInterpreter<A> {
	A interpret(Class<?> clz, Method method);
}
