/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.TextHeaders;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

/**
 * Encodes a {@link StompFrame} or a {@link StompSubframe} into a {@link ByteBuf}.
 */
public class StompSubframeEncoder extends MessageToMessageEncoder<StompSubframe> {

    @Override
    protected void encode(ChannelHandlerContext ctx, StompSubframe msg, List<Object> out) throws Exception {
        if (msg instanceof StompFrame) {
            StompFrame frame = (StompFrame) msg;
            ByteBuf frameBuf = encodeFrame(frame, ctx);
            out.add(frameBuf);
            ByteBuf contentBuf = encodeContent(frame, ctx);
            out.add(contentBuf);
        } else if (msg instanceof StompHeadersSubframe) {
            StompHeadersSubframe frame = (StompHeadersSubframe) msg;
            ByteBuf buf = encodeFrame(frame, ctx);
            out.add(buf);
        } else if (msg instanceof StompContentSubframe) {
            StompContentSubframe stompContentSubframe = (StompContentSubframe) msg;
            ByteBuf buf = encodeContent(stompContentSubframe, ctx);
            out.add(buf);
        }
    }

    private static ByteBuf encodeContent(StompContentSubframe content, ChannelHandlerContext ctx) {
        if (content instanceof LastStompContentSubframe) {
            ByteBuf buf = ctx.alloc().buffer(content.content().readableBytes() + 1);
            buf.writeBytes(content.content());
            buf.writeByte(StompConstants.NUL);
            return buf;
        } else {
            return content.content().retain();
        }
    }

    private static ByteBuf encodeFrame(StompHeadersSubframe frame, ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer();

        buf.writeBytes(frame.command().toString().getBytes(CharsetUtil.US_ASCII));
        buf.writeByte(StompConstants.CR).writeByte(StompConstants.LF);

        TextHeaders headers = frame.headers();
        for (Map.Entry<CharSequence, CharSequence> e: headers.unconvertedEntries()) {
            CharSequence k = e.getKey();
            CharSequence v = e.getValue();
            if (k instanceof AsciiString) {
                AsciiString kstr = (AsciiString) k;
                buf.writeBytes(kstr.array(), kstr.arrayOffset(), kstr.length());
            } else {
                buf.writeBytes(k.toString().getBytes(CharsetUtil.US_ASCII));
            }
            buf.writeByte(StompConstants.COLON);
            if (v instanceof AsciiString) {
                AsciiString vstr = (AsciiString) v;
                buf.writeBytes(vstr.array(), vstr.arrayOffset(), vstr.length());
            } else {
                buf.writeBytes(v.toString().getBytes(CharsetUtil.US_ASCII));
            }
            buf.writeByte(StompConstants.CR).writeByte(StompConstants.LF);
        }
        buf.writeByte(StompConstants.CR).writeByte(StompConstants.LF);
        return buf;
    }
}
