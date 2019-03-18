package com.vic.mynetty.common.exception;

public class ServiceNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1428204905613583580L;
	
    public ServiceNotFoundException() {
        super();
    }

    public ServiceNotFoundException(String msg) {
        super(msg);
    }
}
