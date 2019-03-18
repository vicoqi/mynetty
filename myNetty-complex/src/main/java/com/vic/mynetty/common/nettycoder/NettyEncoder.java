package com.vic.mynetty.common.nettycoder;

import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.nettycoder.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class NettyEncoder extends MessageToByteEncoder<Message> {

    private static final byte[] DELIMETER_BYTES = "$_".getBytes();

    private Codec codec;

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        byte[] data = codec.encode(msg);
        String msgStr = msg.toString();
		String displayStr = "";
		int len = msgStr.length();
		if(len > 800) {
			displayStr = msgStr.substring(0, 800);
			displayStr = displayStr + "...";
		} else {
			displayStr = msgStr;
		}
		log.info("NET_OUT|chnlId=[{}]|msg=[{}]|msglen=[{}bytes]|size=[{}bytes]", ctx.channel().id().asShortText(), displayStr, msgStr.toString().length(), data.length);
		out.writeBytes(data);
        //         out.writeBytes(DELIMETER_BYTES);
    }

}

