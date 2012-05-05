package net.georgewhiteside.android.abstractart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class RomUtil
{
	// This is an internal optimization for the comp/decomp methods.
	// Every element in this array is the binary reverse of its index.
	
	private static final short[] bitrevs = {	
		0,   128, 64,  192, 32,  160, 96,  224, 16,  144, 80,  208, 48,  176, 112, 240, 
		8,   136, 72,  200, 40,  168, 104, 232, 24,  152, 88,  216, 56,  184, 120, 248, 
		4,   132, 68,  196, 36,  164, 100, 228, 20,  148, 84,  212, 52,  180, 116, 244, 
		12,  140, 76,  204, 44,  172, 108, 236, 28,  156, 92,  220, 60,  188, 124, 252, 
		2,   130, 66,  194, 34,  162, 98,  226, 18,  146, 82,  210, 50,  178, 114, 242, 
		10,  138, 74,  202, 42,  170, 106, 234, 26,  154, 90,  218, 58,  186, 122, 250, 
		6,   134, 70,  198, 38,  166, 102, 230, 22,  150, 86,  214, 54,  182, 118, 246, 
		14,  142, 78,  206, 46,  174, 110, 238, 30,  158, 94,  222, 62,  190, 126, 254, 
		1,   129, 65,  193, 33,  161, 97,  225, 17,  145, 81,  209, 49,  177, 113, 241, 
		9,   137, 73,  201, 41,  169, 105, 233, 25,  153, 89,  217, 57,  185, 121, 249, 
		5,   133, 69,  197, 37,  165, 101, 229, 21,  149, 85,  213, 53,  181, 117, 245, 
		13,  141, 77,  205, 45,  173, 109, 237, 29,  157, 93,  221, 61,  189, 125, 253, 
		3,   131, 67,  195, 35,  163, 99,  227, 19,  147, 83,  211, 51,  179, 115, 243, 
		11,  139, 75,  203, 43,  171, 107, 235, 27,  155, 91,  219, 59,  187, 123, 251, 
		7,   135, 71,  199, 39,  167, 103, 231, 23,  151, 87,  215, 55,  183, 119, 247, 
		15,  143, 79,  207, 47,  175, 111, 239, 31,  159, 95,  223, 63,  191, 127, 255  };

	/**
	 * It just copies some junk.
	 * @return number of bytes decompressed, or -1 if there was an error
	 * @author Brassica Oleracea Capitata
	 */
    public static int decompress(int cdata, byte[] output, int maxlen, byte[] rom)
    {
        int start = cdata;
        int bpos = 0, bpos2 = 0;
        short tmp;
        while (unsigned(rom[cdata]) != 0xFF)
        {
            if (cdata >= rom.length)
            {
                return -1;
            }

            int cmdtype = unsigned(rom[cdata]) >> 5;
            int len = (unsigned(rom[cdata]) & 0x1F) + 1;
            
            if (cmdtype == 7)
            {
                cmdtype = (unsigned(rom[cdata]) & 0x1C) >> 2;
                len = ((unsigned(rom[cdata]) & 3) << 8) + unsigned(rom[cdata + 1]) + 1;
                cdata++;
            }
            
            if (bpos + len > maxlen || bpos + len < 0)
            {
                return -1;
            }
            
            cdata++;
            
            if (cmdtype >= 4)
            {
                bpos2 = (unsigned(rom[cdata]) << 8) + unsigned(rom[cdata + 1]);
                if (bpos2 >= maxlen || bpos2 < 0)
                {
                    return -1;
                }
                cdata += 2;
            }

            switch (cmdtype)
            {
                case 0: // uncompressed
                    System.arraycopy(rom, cdata, output, bpos, len);
                    bpos += len;
                    cdata += len;
                    break;
                case 1: // RLE
                    Arrays.fill(output, bpos, bpos + len, rom[cdata]);
                    bpos += len;
                    cdata++;
                    break;
                case 2: // ??? TODO way to do this with Arrays?
                    if (bpos + 2 * len > maxlen || bpos < 0)
                    {
                        return -1;
                    }
                    
                    while (len-- != 0)
                    {
                        output[bpos++] = (byte) unsigned(rom[cdata]);
                        output[bpos++] = (byte) unsigned(rom[cdata + 1]);
                    }
                    cdata += 2;
                    break;
                case 3: // each byte is one more than previous ?
                    tmp = unsigned(rom[cdata++]);
                    while (len-- != 0)
                    {
                        output[bpos++] = (byte) tmp++;
                    }
                    break;
                case 4: // use previous data ?
                    if (bpos2 + len > maxlen || bpos2 < 0)
                    {
                        return -1;
                    }
                    System.arraycopy(output, bpos2, output, bpos, len);
                    bpos += len;
                    break;
                case 5:
                    if (bpos2 + len > maxlen || bpos2 < 0)
                    {
                    	return -1;
                    }
                    
                    while (len-- != 0)
                    {
                        output[bpos++] = reverseByte(output[bpos2++]);
                    }
                    break;
                case 6:
                    if (bpos2 - len + 1 < 0)
                    {
                    	return -1;
                    }
                    
                    while (len-- != 0)
                    {
                        output[bpos++] = output[bpos2--];
                    }
                    break;
                case 7:
                    return -1;
            }
        }
        return bpos;
    }
  
	public static int decompress(int cdata, byte[] output, int maxlen, ByteBuffer rom)
	{
		return decompress(cdata, output, maxlen, rom.array());
	}

    public static short unsigned(byte value)
	{
		return (short) (value & 0xFF);
	}
	
	public static int unsigned(short value)
	{
		return (int) (value & 0xFFFF);
	}
	
	public static long unsigned(int value)
	{
		return (long) (value & 0xFFFFFFFF);
	}
	
	public static int getDecompressedSize(int start, byte[] data)
	{
		int pos = start;
		int bpos = 0, bpos2 = 0;
		
		while (unsigned(data[pos]) != 0xFF) {
			// Data overflow before end of compressed data
			if (pos >= data.length)
				return -8;

			int cmdtype = unsigned(data[pos]) >> 5;
			int len = (unsigned(data[pos]) & 0x1F) + 1;

			if (cmdtype == 7) {
				cmdtype = (unsigned(data[pos]) & 0x1C) >> 2;
				len = ((unsigned(data[pos]) & 3) << 8) + unsigned(data[pos + 1]) + 1;
				pos++;
			}

			if (bpos + len < 0)
				return -1;
			pos++;

			if (cmdtype >= 4) {
				bpos2 = (unsigned(data[pos]) << 8) + unsigned(data[pos + 1]);
				if (bpos2 < 0)
					return -2;
				pos += 2;
			}
			switch (cmdtype) {
			case 0: // Uncompressed block
				bpos += len;
				pos += len;
				break;

			case 1: // RLE
				bpos += len;
				pos += 1;
				break;

			case 2: // 2-byte RLE
				if (bpos < 0)
					return -3;
				bpos += 2 * len;
				pos += 2;
				break;

			case 3: // Incremental sequence
				bpos += len;
				pos += 1;
				break;

			case 4: // Repeat previous data
				if (bpos2 < 0)
					return -4;
				bpos += len;
				break;

			case 5: // Output with bits reversed
				if (bpos2 < 0)
					return -5;
				bpos += len;
				break;

			case 6:
				if (bpos2 - len + 1 < 0)
					return -6;
				bpos += len;
				break;

			case 7:
				return -7;
			}
		}
		return bpos;
	}
	
	public static byte reverseByte(int value)
	{
		return (byte) bitrevs[value & 0xFF];
	}
	
	public static int toHex(int address)
	{
		return address - 0xC00000 + 0x200;
	}
	
	public static int toSnes(int address)
	{
		return address + 0xC00000 - 0x200;
	}
}
