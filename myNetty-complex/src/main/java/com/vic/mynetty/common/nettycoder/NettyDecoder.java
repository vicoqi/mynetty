package com.vic.mynetty.common.nettycoder;

import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.nettycoder.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class NettyDecoder extends ByteToMessageDecoder {

    @Setter
    private Codec codec;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);

        Message obj = codec.decode(data, Message.class);
        log.info("NET_IN|chnlId=[{}]|msg=[{}]|size=[{}bytes]", ctx.channel().id().asShortText(), obj, data.length);
        out.add(obj);
    }

}