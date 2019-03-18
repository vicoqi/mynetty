package com.vic.mynetty.rpc_server;


import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.rpc_common.service.MethodInvocation;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
public class ReportService extends RequestService {
    @Override
    protected boolean isServiceClazz(Class<?> candidate) {
        if (candidate.isInterface()) {
            return false;
        }
        Class<?>[] interfaces = candidate.getInterfaces();
        if (interfaces == null || interfaces.length == 0) {
            return false;
        }
        Class<?> superClz = candidate.getInterfaces()[0];
        if (superClz != null) {
            Mapping mapping = superClz.getAnnotation(Mapping.class);
            if (mapping != null) {
                Type typeValue = mapping.type();
                return typeValue == Type.REPORT;
            }
        }
        return false;
    }

    public void report(Mapping.Model mapping, Object[] parameters) {
        MethodInvocation methodInvocation = mapping2MethodInvokeMap.get(mapping);
        if (methodInvocation == null) {
            log.error("SERVICE_NOT_FOUND_EXCEPTION_4_MAPPING|mapping=[{}]", mapping);
            return;
        }

        try {
            Method method = methodInvocation.getMethod();
            method.invoke(methodInvocation.getInstance(), parameters);
        } catch (Exception e) {
            log.error("EXCEPTION_WHEN_INVOKING_SERVICE", e);
        }
    }

    @Override
    protected String[] getScanningPackages() {
        return serverContext.getServerConfig().getReportPackages();
    }
}
