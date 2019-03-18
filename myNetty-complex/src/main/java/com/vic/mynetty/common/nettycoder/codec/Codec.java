package com.vic.mynetty.common.nettycoder.codec;
/**
 * singleton
 * @author donglongxiang
 *
 */
public interface Codec {
	<A, B> B encode(A instance);

	<B> B[] encode(Object[] instances);

	<A, B> A decode(B serialized, Class<A> clazz);

	<B> Object[] decode(B[] serialized, Class<?>[] clazz);
}
