package com.vic.mynetty.rpc_common.service;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class MethodInvocation {
	private Object instance;
	private Method method;
	private Class<?>[] parameterTypes;
}
