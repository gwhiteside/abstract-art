package net.starmen.pkhack;

// Original code by AnyoneEB
// Code released under the GPL - http://www.gnu.org/licenses/gpl.txt

public class HackModule {
	/**
     * Reads a two bit per pixel (2BPP) area from <code>source</code> to the
     * given byte array. This area represents a 8x8 four color image. Note that
     * the size of a 2BPP image is 16 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @param bitOffset number of bits to left-shift <code>target</code>
     * @return number of bytes read
     */
    public static int read2BPPArea(byte[] target, byte[] source, int off,
        int x, int y, int stride, int bitOffset)
    {
        if (bitOffset < 0)
            bitOffset = 0;
        int offset = off;
        for (int i = 0; i < 8; i++)
        {
            for (int k = 0; k < 2; k++)
            {
                byte b = source[offset++];
                for (int j = 0; j < 8; j++)
                {
                    // target[(7 - j) + x][i + y] |= (byte) (((b & (1 << j)) >> j) << (k + bitOffset));
                	// changed to just write to a flat array
                    target[(7 - j) + x + (i + y) * stride] |= (byte) (((b & (1 << j)) >> j) << (k + bitOffset));
                }
            }
        }
        return offset - off;
    }
    
    /**
     * Reads a four bit per pixel (4BPP) area from <code>source</code> to the
     * given byte array. This area represents a 8x8 four color image. Note that
     * the size of a 4BPP image is 32 bytes.
     * 
     * @param target byte array to write to
     * @param source reading buffer
     * @param off index to start reading from
     * @param x x-offset on image to write to
     * @param y y-offset on image to write to
     * @param bitOffset number of bits to left-shift <code>target</code>
     * @return number of bytes read
     */
    public static int read4BPPArea(byte[] target, byte[] source, int off,
        int x, int y, int stride, int bitOffset)
    {
        if (bitOffset < 0)
            bitOffset = 0;
        read2BPPArea(target, source, off, x, y, stride, bitOffset);
        read2BPPArea(target, source, off + 16, x, y, stride, bitOffset + 2);
        return 32;
    }
}
