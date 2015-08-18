package com.example.mp3reader;

import java.nio.ByteBuffer;

public class Frame
{
	public final static int FRAME_HEADER_SIZE = 4;
	//private int frameHeader;
	//private byte[] frameHeader;
	private int mpegVersionIndex; // 0 - MPEG-2.5, 1 - not used, 2 - MPEG-2, 3 - MPEG-1
	private int layerTypeIndex;
	private boolean protection;
	private int bitrateIndex;
	private int samplingRateIndex;
	private boolean padding;
	private boolean privateBit;
	private int chanelModeIndex; // 0 - Stereo, 1 - Join stereo, 2 - Dual chanel, 3 - Mono
	private int modeExtension;
	private boolean copyright;
	private boolean original;
	private int emphasisIndex;
	
	private int frameSize;
	private int frameLength;
	
	private int samplingRateValue;
	private int bitrateValue;
	private int samplesPerFrame;
	
	public static int samplinRate(int mpegVersionIndex, int samplinRateIndex)
	{
		int[][] samplingRates = new int[][] {new int[] {11025, 12000, 8000},
											 null,
											 new int[] {22050, 24000, 16000},
											 new int[] {44100, 48000, 32000}};
		
		return samplingRates[mpegVersionIndex][samplinRateIndex];
	}
	
	public static int bitrate(int mpegVersionIndex, int layerTypeIndex, int bitrateIndex)
	{
		if(mpegVersionIndex == 3)
		{
			int[][] array = new int[][] {null,
										 new int[] {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320},
										 new int[] {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},
										 new int[] {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448}};
			return array[layerTypeIndex][bitrateIndex]*1000;
		}
		else
		{
			if(layerTypeIndex == 3)
			{
				int[] array = new int[] {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256};
				return array[bitrateIndex]*1000;
			}
			else
			{
				int[] array = new int[] {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};
				return array[bitrateIndex]*1000;
			}
		}
	}
	
	public static int computeSamplesPerFrame(int mpegVersionIndex, int layerTypeIndex)
	{
		int[][] array = new int[][] {new int[] {0, 576, 1152, 384},
			 						 null,
			 						 new int[] {0, 576, 1152, 384},
			 						 new int[] {0, 1152, 1152, 384}};

		return array[mpegVersionIndex][layerTypeIndex];
	}
	
	public void computeParameters(int frameHeader)
	{
		this.mpegVersionIndex =  frameHeader >> 19 & 0b11;
		this.layerTypeIndex =    frameHeader >> 17 & 0b11;
		this.protection =       (frameHeader >> 16 & 0b1) == 0b1;
		this.bitrateIndex =      frameHeader >> 12 & 0b1111;
		this.samplingRateIndex = frameHeader >> 10 & 0b11;
		this.padding =          (frameHeader >> 9  & 0b1) == 0b1;
		this.privateBit =       (frameHeader >> 8  & 0b1) == 0b1;
		this.chanelModeIndex =   frameHeader >> 6  & 0b11;
		this.modeExtension =     frameHeader >> 4  & 0b11;
		this.copyright =        (frameHeader >> 3  & 0b1) == 0b1;
		this.original =         (frameHeader >> 2  & 0b1) == 0b1;
		this.emphasisIndex =     frameHeader & 0b11;
		
		this.samplingRateValue = samplinRate(mpegVersionIndex, samplingRateIndex);
		this.bitrateValue = bitrate(mpegVersionIndex, layerTypeIndex, bitrateIndex);
		
		this.frameSize = 144 * this.bitrateValue / this.samplingRateValue;
		
		if(this.padding)
			this.frameSize++;
		
		this.samplesPerFrame = computeSamplesPerFrame(mpegVersionIndex, layerTypeIndex);
		
		this.frameLength = (int) ((1000.0f / (float)this.samplingRateValue) * (float)this.samplesPerFrame);
	}
	
	public Frame(byte[] frameHeaderBytes)
	{
		if(frameHeaderBytes != null)
		{
			int frameHeader = ByteBuffer.wrap(frameHeaderBytes).getInt();
			this.computeParameters(frameHeader);
		}
	}
	
	public Frame()
	{
		this.mpegVersionIndex = 0;
		this.layerTypeIndex = 0;
		this.protection = false;
		this.bitrateIndex = 0;
		this.samplingRateIndex = 0;
		this.padding = false;
		this.privateBit = false;
		this.chanelModeIndex = 0;
		this.modeExtension = 0;
		this.copyright = false;
		this.original = false;
		this.emphasisIndex = 0;
		this.frameSize = 0;
		this.frameLength = 0;
		this.samplingRateValue = 0;
		this.bitrateValue = 0;
		this.samplesPerFrame = 0;
	}

	public int getMpegVersionIndex() {
		return mpegVersionIndex;
	}

	public int getLayerTypeIndex() {
		return layerTypeIndex;
	}

	public boolean isProtection() {
		return protection;
	}

	public int getBitrateIndex() {
		return bitrateIndex;
	}

	public int getSamplingRateIndex() {
		return samplingRateIndex;
	}

	public boolean isPadding() {
		return padding;
	}

	public boolean isPrivateBit() {
		return privateBit;
	}

	public int getChanelModeIndex() {
		return chanelModeIndex;
	}

	public int getModeExtension() {
		return modeExtension;
	}

	public boolean isCopyright() {
		return copyright;
	}

	public boolean isOriginal() {
		return original;
	}

	public int getEmphasisIndex() {
		return emphasisIndex;
	}

	public int getFrameSize() {
		return frameSize;
	}

	public int getFrameLength() {
		return frameLength;
	}

	public int getSamplingRateValue() {
		return samplingRateValue;
	}

	public int getBitrateValue() {
		return bitrateValue;
	}

	public int getSamplesPerFrame() {
		return samplesPerFrame;
	}
}