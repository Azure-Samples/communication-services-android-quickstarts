package com.microsoft.acs.calling.rawvideo;

import java.nio.ByteBuffer;

public class BufferExtensions
{
    public static byte[] GetArrayBuffer(ByteBuffer byteBuffer)
    {
        byte[] arrayBuffer = new byte[byteBuffer.capacity()];
        byteBuffer.get(arrayBuffer);

        return arrayBuffer;
    }
}
