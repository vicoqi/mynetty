package com.vic.mynetty.common.exception;

public class ServiceRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1428204905613583580L;
	
    public ServiceRuntimeException() {
        super();
    }

    public ServiceRuntimeException(String msg) {
        super(msg);
    }
}
