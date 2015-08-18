package com.example.mp3reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class MP3
{
	//private boolean id3v1;
	private byte[] headData;
	private byte[] id3v1Data;
	private File mp3File;
	private int mpegVersionIndex;
	private int chanelModeIndex;
	private int bitrateValue;
	private String encodeMode;
	private int framesCount;
	private int durationSec;
	private int firstFrameOffset;
	private int lastFrameOffset;
	private int lastFrameSize;
	private int encodeModeTagOffset;
	private int framesCountValueOffset;
	private int msPerFrame;
	private int framesPerSec;
	//private int framesLength;
	
	private int computeVbrTagOffset(int mpegVersionIndex, int chanelModeIndex, int firstFrameOffset)
	{
		int vbrOffset = firstFrameOffset+Frame.FRAME_HEADER_SIZE;
		
		if(chanelModeIndex == 3)
		{
			if(mpegVersionIndex == 3)
			{
				vbrOffset += 17;
			}
			else
			{
				vbrOffset += 9;
			}
		}
		else
		{
			if(mpegVersionIndex == 3)
			{
				vbrOffset += 32;
			}
			else
			{
				vbrOffset += 17;
			}
		}
		
		return vbrOffset;
	}
	
	private int computeFramesCountValueOffset(byte[] encModeMarker, int vbrTagOffset)
	{
		int framesCountValueOffset = 14;
		String encMode = new String(encModeMarker);
		
		if("Xing".equals(encMode) || "Info".equals(encMode))
		{
			framesCountValueOffset = 8;
		}
		
		framesCountValueOffset += vbrTagOffset;
		
		return framesCountValueOffset;
	}
	
	private int findFirstFrameOffset(File mp3File, RandomAccessFile raf) throws IOException
	{
		byte[] d = new byte[10];
		int offset = 0;
		while(true)
		{
			raf.seek(offset);
			raf.read(d);
			
			if(isFrameMarker(d))
			{
				break;
			}
			else
			{
				if(isID3v2Marker(d))
				{
					offset = (int) (raf.getFilePointer()+ID3v2Tag.computeDataLength(d));
				}
				else
				{
					offset++;
				}
			}
		}
		
		return offset;
	}
	
	/*public long[] findLastFrameSegment(boolean isID3v1, RandomAccessFile raf) throws IOException
	{
		long[] result = new long[2];
		byte[] d = new byte[Frame.FRAME_HEADER_SIZE];
		long offset = this.mp3File.length()-d.length;
		if(isID3v1)
		{
			offset -= ID3v1Tag.ID3V1_TAG_LENGTH;
		}
		
		while(offset > 0)
		{
			raf.seek(offset);
			raf.read(d);
			if(this.isFrameMarker(d))
			{
				Frame f = new Frame(d);
				result[0] = offset;
				result[1] = f.getFrameSize();
				break;
			}
			offset--;
		}
		
		return result;
	}*/
	
	private boolean isVBRMarker(byte[] vbrMarker)
	{
		String s = new String(vbrMarker);
		return "VBRI".equals(s) || "Xing".equals(s) || "Info".equals(s);
	}
	
	private boolean isFrameMarker(byte[] bytes)
	{
		byte lByte = (byte) (bytes[1] >> 0xf);
		return (bytes[0] & 0xff) == 0xff && ((lByte & 0xf) == 0xf || (lByte & 0xe) == 0xe);
	}
	
	private boolean isID3v2Marker(byte[] headBytes)
	{
		return headBytes[0] == 0x49 && headBytes[1] == 0x44 && headBytes[2] == 0x33;
	}
	
	private boolean isID3v1Marker(byte[] headBytes)
	{
		return headBytes[0] == 0x54 && headBytes[1] == 0x41 && headBytes[2] == 0x47;
	}
	
	public byte[] getData(int offset, int length) throws IOException
	{
		byte[] result = new byte[length];
		InputStream fis = new FileInputStream(this.mp3File);
		fis.skip(offset);
		fis.read(result);
		fis.close();
		return result;
	}
	
	private int[] getNFrameSegment(int firstFrameOffset, int n) throws IOException
	{
		int[] result = null;
		byte[] frameHeader = new byte[4];
		int offset = firstFrameOffset;
		int frameN = 0;
		Frame f = new Frame();
		RandomAccessFile rafReader = new RandomAccessFile(this.mp3File, "r");
		
		while(offset <= this.mp3File.length())
		{
			rafReader.seek(offset);
			rafReader.read(frameHeader);
			if(isFrameMarker(frameHeader))
			{
				f.computeParameters(ByteBuffer.wrap(frameHeader).getInt());
				if(frameN == n)
				{
					break;
				}
				frameN++;
				offset+=f.getFrameSize();
			}
			else
			{
				break;
			}
		}
		
		result = new int[3];
		result[0] = offset;
		result[1] = f.getFrameSize();
		result[2] = frameN;
		
		rafReader.close();
		return result;
	}
	
	private int[] getFramesSegmentStartWithN(int n) throws IOException
	{
		if(n < 0 || n > this.framesCount)
			return null;
		
		boolean nFound = false;
		int[] result =  this.getNFrameSegment(this.firstFrameOffset, n);
		
		result[1] = this.lastFrameOffset + this.lastFrameSize;

		
		return result;
	}
	
	private int[] getFramesSegmentEndWithN(int n) throws IOException
	{
		if(n < 0 || n > this.framesCount)
			return null;
		
		boolean nFound = false;
		int[] result = new int[2];
		result[0] = this.firstFrameOffset;
		int[] frameSegment =  this.getNFrameSegment(this.firstFrameOffset, n);
		
		result[1] = frameSegment[0] + frameSegment[1];
		
		return result;
	}
	
	private int[] getFramesSegment(int n, int count) throws IOException
	{
		if(n < 0 || count < 1 || n > this.framesCount)
			return null;
		
		boolean nFound = false;
		int[] result =  this.getNFrameSegment(this.firstFrameOffset, n);
		if(n+count > this.framesCount)
		{
			result[1] = this.lastFrameOffset + this.lastFrameSize;
		}
		else
		{
			int[] frameSegment = this.getNFrameSegment(this.firstFrameOffset, n+count);
			result[1] = (frameSegment[0] + frameSegment[1]) - result[0];
		}
		
		return result;
	}
	
	/*public void eraseFrameSegment(File mp3File, long firstFrameOffset, int n, int count) throws IOException
	{
		long[] segment = this.getFramesSegment(mp3File, firstFrameOffset, n, count);
		this.eraseBytes(mp3File, (int)segment[0], (int)segment[1]);
		if(!this.encodeMode.equals("CBR"))
		{
			RandomAccessFile rafWriter = new RandomAccessFile(mp3File, "rw");
			int framesCount = (int) (this.framesCount - segment[2]);
			rafWriter.seek(this.framesCountValueOffset);
			rafWriter.writeInt(framesCount);
			rafWriter.close();
		}
	}*/
	
	/*private long[] getFramesSegmentBySec(int sec) throws IOException
	{
		long[] result = null;
			
		result = getFramesSegment(this.mp3File, this.framesPerSec*sec, this.framesPerSec);
		
		return result;
	}*/
	
	/*public void eraseFramesByTime(int startTime, int timeLength) throws IOException
	{
		long[] segment = getFramesSegmentByTime(startTime, timeLength);
		eraseBytes(this.mp3File, (int)segment[0], (int)segment[1]);
		if(!this.encodeMode.equals("CBR"))
		{
			RandomAccessFile rafWriter = new RandomAccessFile(mp3File, "rw");
			int framesCount = (int) (this.framesCount - segment[2]);
			rafWriter.seek(this.framesCountValueOffset);
			rafWriter.writeInt(framesCount);
			rafWriter.close();
		}
		this.successivelyReading(this.mp3File);
	}*/
	
	public int[] getFramesSegmentByTime(int startTime, int miliseconds) throws IOException
	{
		if(startTime < 0)
			return null;
		
		if(miliseconds < 1)
			return null;
		
		int[] result = new int[3];
		
		int startFrame = Math.round(startTime/this.msPerFrame);
		int framesCount = Math.round(miliseconds/this.msPerFrame);
		
		int[] start = getFramesSegment(startFrame, framesCount);
		//int[] end = getFramesSegment(this.framesPerSec*(startTime+miliseconds), this.framesPerSec);
		
		result[0] = start[0];
		result[1] = start[1];//end[0] - start[0];
		result[2] = framesCount;//(startTime+miliseconds)*this.framesPerSec;
		return result;
	}
	
	public int[] getFramesSegmentStartWith(int sec) throws IOException
	{
		int[] result = null;
		int n = this.framesPerSec * sec;
		result = getFramesSegmentStartWithN(n);
		
		return result;
	}
	
	public int[] getFramesSegmentEndWith(int sec) throws IOException
	{
		int[] result = null;
		int n = this.framesPerSec * (sec-1);
		result = getFramesSegmentEndWithN(n);
		
		return result;
	}
	
	/*static void erase(File file, int start, int end) throws IOException
	{
		File tempFile = new File(appFolder, "src\\temp.mp3");
		FileOutputStream fos = new FileOutputStream(tempFile);
		RandomAccessFile rafReader = new RandomAccessFile(file, "r");
		
		byte[] newData = new byte[(int) (file.length() - (end - start + 1))];
		
		rafReader.seek(0);
		rafReader.read(newData, 0, start);
		
		rafReader.seek(end+1);
		rafReader.read(newData, start, (int) (file.length() - rafReader.getFilePointer()));
		
		fos.write(newData);
		
		fos.close();
		rafReader.close();
		
		file.delete();
		tempFile.renameTo(new File("src\\"+file.getName()));
	}*/
	
	public void rewriteFile(File file, byte[] data) throws IOException
	{
		File tempFile = new File(file.getParent(), "temp.mp3");
		FileOutputStream fos = new FileOutputStream(tempFile);
		fos.write(data);
		fos.close();
		file.delete();
		tempFile.renameTo(new File(file.getParent(), file.getName()));
	}
	
	private void eraseBytes(File file, int start, int length) throws IOException
	{
		if(length < 1)
			return;
		
		//File tempFile = new File(file.getParent(), "temp.mp3");
		//FileOutputStream fos = new FileOutputStream(tempFile);
		RandomAccessFile rafReader = new RandomAccessFile(file, "r");
		
		byte[] newData = new byte[(int) (file.length() - length)];
		
		rafReader.seek(0);
		rafReader.read(newData, 0, start);
		
		rafReader.seek(start+length);
		rafReader.read(newData, start, (int) (file.length() - rafReader.getFilePointer()));
		
		//fos.write(newData);
		
		//fos.close();
		rafReader.close();
		
		//file.delete();
		
		//tempFile.renameTo(new File(file.getParent(), file.getName()));
		
		rewriteFile(file, newData);
		rewriteVBRInfo();
	}
	
	/*private boolean findID3v1() throws IOException
	{
		RandomAccessFile rafReader = new RandomAccessFile(this.mp3File, "r");
		rafReader.seek(this.mp3File.length()-ID3v1Tag.ID3V1_TAG_LENGTH);
		byte[] id3v2Marker = new byte[ID3v1Tag.ID3V1_MARKER_LENGTH];
		rafReader.read(id3v2Marker);
		rafReader.close();
		return this.isID3v1Marker(id3v2Marker);
	}*/
	
	public void rewriteVBRInfo() throws IOException
	{
		if(!this.encodeMode.equals("CBR"))
		{
			int framesCount = getNFrameSegment(this.firstFrameOffset, -1)[2];
		
			RandomAccessFile rafWriter = new RandomAccessFile(mp3File, "rw");
			rafWriter.seek(this.framesCountValueOffset);
			rafWriter.writeInt(framesCount);
			rafWriter.close();
		}
	}
	
	private void successivelyReading(File mp3File) throws IOException
	{
		RandomAccessFile rafReader = new RandomAccessFile(mp3File, "r");
		this.firstFrameOffset = findFirstFrameOffset(mp3File, rafReader);
		rafReader.seek(this.firstFrameOffset);
		byte[] frameHeader = new byte[4];
		rafReader.read(frameHeader);
		
		Frame f = new Frame(frameHeader);
		
		this.chanelModeIndex = f.getChanelModeIndex();
		this.mpegVersionIndex = f.getMpegVersionIndex();
		this.bitrateValue = f.getBitrateValue();
		this.msPerFrame = f.getFrameLength();
		this.framesPerSec = 1000/this.msPerFrame;
		
		this.encodeModeTagOffset = computeVbrTagOffset(this.mpegVersionIndex, this.chanelModeIndex, this.firstFrameOffset);
		rafReader.seek(this.encodeModeTagOffset);
		byte[] encodeTagMarker = new byte[4];
		rafReader.read(encodeTagMarker);
		
		if(isVBRMarker(encodeTagMarker))
		{
			this.firstFrameOffset += f.getFrameSize();
			this.bitrateValue = -1;
			this.encodeMode = new String(encodeTagMarker);
			this.framesCountValueOffset = computeFramesCountValueOffset(encodeTagMarker, this.encodeModeTagOffset);
			rafReader.seek(this.framesCountValueOffset);
			this.framesCount = rafReader.readInt();
			
			this.durationSec = (this.framesCount*f.getSamplesPerFrame())/f.getSamplingRateValue();
		}
		else
		{
			this.encodeMode = "CBR";
			
			this.framesCount = getNFrameSegment(this.firstFrameOffset, -1)[2];
			
			int[] lastFrameSegment = getNFrameSegment(this.firstFrameOffset, -1);
			this.lastFrameOffset = lastFrameSegment[0];
			this.lastFrameSize = lastFrameSegment[1];
			
			int audioDataSize = (this.lastFrameOffset+this.lastFrameSize) - this.firstFrameOffset;
			this.durationSec = (int)(((float)audioDataSize/(float)this.bitrateValue)*8.0f);
		}
		
		this.headData = new byte[this.firstFrameOffset];
		rafReader.seek(0);
		rafReader.read(this.headData);
		
		rafReader.seek(mp3File.length() - ID3v1Tag.ID3V1_TAG_LENGTH);
		this.id3v1Data = new byte[ID3v1Tag.ID3V1_TAG_LENGTH];
		rafReader.read(this.id3v1Data);
		
		if(!isID3v1Marker(this.id3v1Data))
		{
			this.id3v1Data = new byte[0];
		}
		
		rafReader.close();
	}
	
	public MP3(File mp3File) throws IOException
	{
		this.mp3File = mp3File;
		this.successivelyReading(mp3File);
	}

	public int getMpegVersionIndex() {
		return mpegVersionIndex;
	}

	public int getChanelModeIndex() {
		return chanelModeIndex;
	}

	public int getBitrateValue() {
		return bitrateValue;
	}

	public String getEncodeMode() {
		return encodeMode;
	}

	public int getFramesCount() {
		return framesCount;
	}

	public int getDuration() {
		return durationSec;
	}

	public long getFirstFrameOffset() {
		return firstFrameOffset;
	}

	public long getEncodeModeTagOffset() {
		return encodeModeTagOffset;
	}

	public long getFramesCountValueOffset() {
		return framesCountValueOffset;
	}

	public int getMsPerFrame() {
		return msPerFrame;
	}

	public byte[] getHeadData() {
		return headData;
	}

	public byte[] getId3v1Data() {
		return id3v1Data;
	}

	public File getMp3File() {
		return mp3File;
	}
}