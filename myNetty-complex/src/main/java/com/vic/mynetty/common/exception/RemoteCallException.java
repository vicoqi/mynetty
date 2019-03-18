package com.vic.mynetty.common.exception;

public class RemoteCallException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1428204905613583580L;
	
    public RemoteCallException() {
        super();
    }

    public RemoteCallException(String msg) {
        super(msg);
    }
}
