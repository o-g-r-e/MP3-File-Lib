package com.example.mp3reader;


public class ID3v2Tag
{
	//public final static int ID3V2_TAG_OFFSET = 0;
	private final int ID3V2_HEADER_LENGTH = 10;
	private byte[] header;
	private byte[] data;
	
	public ID3v2Tag()
	{
		this.header = new byte[this.ID3V2_HEADER_LENGTH];
	}
	
	public static int computeDataLength(byte[] id3v2Header)
	{
		byte[] length = new byte[4];
		System.arraycopy(id3v2Header, 6, length, 0, length.length);
		
		int l = 0;
		
		for(int i = 0; i < 4; i++)
		{
			l += length[i] << 7*(3-i);
		}
		
		return l;
	}

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte[] header) {
		this.header = header;
		data = new byte[this.computeDataLength(this.header)];
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public int getVersion()
	{
		return header[3] & 0xff;
	}
	
	public int getSubVersion()
	{
		return header[4] & 0xff;
	}
	
	public int getFlag()
	{
		return header[5] & 0xff;
	}
}