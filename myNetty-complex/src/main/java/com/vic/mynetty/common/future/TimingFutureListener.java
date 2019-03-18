package com.vic.mynetty.common.future;

import lombok.Getter;
import lombok.Setter;

public abstract class TimingFutureListener<T> implements FutureListener<T> {
	@Setter
    @Getter
	private long expireTm;
}
