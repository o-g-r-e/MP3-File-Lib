package com.example.mp3reader;


public class ID3v1Tag
{
	public final static int ID3V1_MARKER_LENGTH = 3;
	public final static int ID3V1_TAG_LENGTH = 128;
	private byte[] data;
	private long id3v1TagOffset;
	
	public ID3v1Tag()
	{
		data = new byte[(int) ID3v1Tag.ID3V1_TAG_LENGTH];
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public long getId3v1TagOffset() {
		return id3v1TagOffset;
	}

	public void setId3v1TagOffset(long fileSize) {
		this.id3v1TagOffset = fileSize-ID3v1Tag.ID3V1_TAG_LENGTH;
	}
}