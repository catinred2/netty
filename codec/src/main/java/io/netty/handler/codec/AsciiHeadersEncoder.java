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

package io.netty.handler.codec;


import io.netty.buffer.ByteBuf;

public final class AsciiHeadersEncoder implements TextHeaderProcessor {

    private final ByteBuf buf;

    public AsciiHeadersEncoder(ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public boolean process(CharSequence name, CharSequence value) throws Exception {
        final ByteBuf buf = this.buf;
        final int nameLen = name.length();
        final int valueLen = value.length();
        final int entryLen = nameLen + valueLen + 4;
        int offset = buf.writerIndex();
        buf.ensureWritable(entryLen);
        offset = writeAscii(buf, offset, name);
        buf.setByte(offset ++, ':');
        buf.setByte(offset ++, ' ');
        offset = writeAscii(buf, offset, value);
        buf.setByte(offset ++, '\r');
        buf.setByte(offset ++, '\n');
        buf.writerIndex(offset);
        return true;
    }

    private static int writeAscii(ByteBuf buf, int offset, CharSequence name) {
        int length = name.length();
        if (name instanceof AsciiString) {
            ((AsciiString) name).copy(0, buf, offset, length);
            offset += length;
        } else {
            for (int i = 0; i < length; i ++) {
                char ch = name.charAt(i);
                buf.setByte(offset ++, ch < 256? (byte) ch : '?');
            }
        }
        return offset;
    }
}
