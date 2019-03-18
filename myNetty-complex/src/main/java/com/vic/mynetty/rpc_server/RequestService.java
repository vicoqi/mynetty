package com.vic.mynetty.rpc_server;


import com.vic.mynetty.common.declarative.Mapping;
import com.vic.mynetty.common.declarative.Type;
import com.vic.mynetty.common.exception.ServiceNotFoundException;
import com.vic.mynetty.common.exception.ServiceRuntimeException;
import com.vic.mynetty.rpc_common.service.AbstractServerService;
import com.vic.mynetty.rpc_common.service.MethodInvocation;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RequestService extends AbstractServerService {

    protected Map<Mapping.Model, MethodInvocation> mapping2MethodInvokeMap
        = new HashMap<Mapping.Model, MethodInvocation>();

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
                return typeValue == Type.REQUEST;
            }
        }
        return false;
    }

    @Override
    protected void initCandidate(Class<?> candidate) {
        Class<?> superInterface = candidate.getInterfaces()[0];
        Method[] interfaceMethods = superInterface.getMethods();
        for (Method m : interfaceMethods) {
            Mapping.Model mapping = new Mapping.Interpreter().interpret(superInterface, m);
            MethodInvocation methodInvocation = new MethodInvocation();
            try {
                methodInvocation.setInstance(candidate.newInstance());
            } catch (Exception e) {
                log.error("Exception when init service", e);
            }
            methodInvocation.setMethod(m);
            methodInvocation.setParameterTypes(m.getParameterTypes());
            mapping2MethodInvokeMap.put(mapping, methodInvocation);
        }
    }

    public Object request(Mapping.Model mapping, Object[] parameters) throws Exception {
        MethodInvocation methodInvocation = mapping2MethodInvokeMap.get(mapping);
        if (methodInvocation == null) {
            throw new ServiceNotFoundException(
                String.format("NO_SUCH_SERVICE_4_MAPPING|mapping=[%s]", mapping.toString()));
        }
        Method method = methodInvocation.getMethod();
        Object ret = null;
        try {
            ret = method.invoke(methodInvocation.getInstance(), parameters);
        } catch (Exception e) {
            log.error("Exception when invoking service", e);
            throw new ServiceRuntimeException(
                String.format("Exception when invoking service|exception=[{}]|msg=[{}]", e.getClass().getName(),
                    e.getMessage()));
        }
        return ret;
    }

    @Override
    protected String[] getScanningPackages() {
        return serverContext.getServerConfig().getRequestPackages();
    }

    @Override
    protected void stopInner() {
        // TODO Auto-generated method stub

    }
}
