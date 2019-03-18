package com.vic.mynetty.common.nettycoder.codec;

public class ProtostuffCodec implements Codec {
	@Override
	public <A, B> B encode(A instance) {
		return (B) ProtostuffUtil.serializer(instance);
	}

	@Override
	public <B> B[] encode(Object[] instances) {
		byte[][] serializedArr = new byte[instances.length][];
		for (int i = 0; i < instances.length; i++) {
			serializedArr[i] = encode(instances[i]);
		}
		return (B[]) serializedArr;
	}

	@Override
	public <A, B> A decode(B serialized, Class<A> clazz) {
		return ProtostuffUtil.deserializer((byte[]) serialized, clazz);
	}

	@Override
	public <B> Object[] decode(B[] serialized, Class<?>[] clazzArr) {
		Object[] tArr = new Object[serialized.length];
		for (int i = 0; i < serialized.length; i++) {
			tArr[i] = decode(serialized[i], clazzArr[i]);
		}
		return tArr;
	}
}
