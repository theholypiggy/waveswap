import java.io.*;

import net.beadsproject.beads.data.SampleAudioFormat;
import net.beadsproject.beads.data.audiofile.AudioFileType;
import net.beadsproject.beads.data.audiofile.FileFormatException;
import net.beadsproject.beads.data.audiofile.OperationUnsupportedException;
import net.beadsproject.beads.data.audiofile.WavFileReaderWriter;

/**
 * Parses a file to eventually be converted to a sound wave.
 * @authors Hannah Roth, Matthew Montera
 * @date September 15th, 2015
 */
public class Parser {
	
	//Need ID tag on front of audio, so that the listener knows when the 
	//actual data begins being sent.  This can either be independent of the
	//lowFrequency/sensitivity chosen by the sender, such as a unique sequence
	//of bytes used for every transmission, or dependent as in a function which
	//the sender and receiver receive the same result from depending on the 
	//lowFrequency, transmissionSpeed, and sensitivity.
	
	private int getBits(int value, int numBits)
	{
		
	}
	
	/**
	 * Converts a byte array into an audio file
	 * 
	 * @param filePath Path to place the audio file
	 * @param fileName Audio file name
	 * @param transmissionSpeed Bits to read per transmission e.g., 2 = 00, 01, 10, 11
	 * @param lowFrequency Frequency that transmission begins at, e.g., lowFrequency = 10Hz, 00 = 10Hz
	 * @param sensitivity Difference between frequencies that hardware can detect
	 */
	public static void createAudioFile(byte[] bytes, String filePath, int transmissionSpeed, float lowFrequency, float sensitivity) {
		int numBytes = bytes.length;
		int numBits = numBytes * 8;
		int totalBits = numBits + (numBits % transmissionSpeed); //Padding at the end of the transmission

		// First dimension is the number of channels (frequency, overlay)
		// Second dimension is the number of frames (time)
		float[][] data = new float[1][totalBits/transmissionSpeed];
		int byteIndex = 0;
		int bitIndex = 0;
		int counter = 0;
		int maxIterations = totalBits/transmissionSpeed;
		
		System.out.println("NumBytes: " + numBytes + " numBits: " + numBits + " totalBits: " + totalBits);
		
		while (true) {
			if (counter == maxIterations) {
				break;
			}
			
			Byte currentByte = bytes[byteIndex];
			float frequency = lowFrequency;
			for (int i = 0; i < transmissionSpeed; i++) {
				if (bitIndex > 7) {
					currentByte = bytes[++byteIndex];
					bitIndex = 0;
				}
				int currentBit = (currentByte & (1 << i)) == 0 ? 0 : 1;
				if (currentBit == 1) {
					frequency += Math.pow(2, transmissionSpeed - i - 1) * sensitivity;
				}
				bitIndex++;
			}
			data[0][counter++] = frequency; // does this work? yes.
		}
		
		//float[][] data, java.lang.String filename, AudioFileType type, SampleAudioFormat saf) throws java.io.IOException, OperationUnsupportedException, FileFormatException
		WavFileReaderWriter wfrw = new WavFileReaderWriter();
		try {
			SampleAudioFormat format = new SampleAudioFormat(100, 2, 1);
			wfrw.writeAudioFile(data, filePath, AudioFileType.WAV, format);
			System.out.println("hello");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OperationUnsupportedException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Decode the audio file AFTER relevant bits have been taken from the front
	 * of the transmission that determine:
	 *   Size
	 *   TransmissionSpeed
	 *   LowFrequency
	 *   Sensitivity
	 */
	public static byte[] decodeAudioFile(float[][] audioMatrix, int size, int transmissionSpeed, float lowFrequency, float sensitivity) {
		//Restrict to one channel for now until we know what they are for.
		byte[] bytes = new byte[size];
		
		byte currentByte = 0;
		int currentByteIndex = 0;
		float currentFrequency;
		int bitsInCurrentByte = 0;
		
		for(int index = 0; index < audioMatrix[0].length; index++) {
			
			currentFrequency = audioMatrix[0][index];
			
			float currentLowOffset = currentFrequency - lowFrequency;
			int currentBits = currentLowOffset / sensitivity;  //Should be an exact division
			int currentBitsSize = transmissionSpeed;
			
			while(currentBitsSize > 0)
			{
				if(bitsInCurrentByte + currentBitsSize >= 8)
				{
					currentByte &= (currentBits >> (currentBitsSize + bitsInCurrentByte - 8));
					currentBits = currentBits >> (8 - bitsInCurrentByte);
					bytes[currentByteIndex] = currentByte;
					currentByteIndex++;
					currentByte = 0;
					currentBitsSize -= (8 - bitsInCurrentByte);
					bitsInCurrentByte = 0;
				}
				else
				{
				    currentByte &= (currentBits << (8 - bitsInCurrentByte - currentBitsSize));
				    bitsInCurrentByte += currentBitsSize;
				    currentBitsSize = 0;
				}
			}
		}
		return bytes;
	}
	
	/**
	 * Main method takes file to parse as parameter.
	 * @param args is the list of parameters
	 */
	public static void main(String[] args) {
		if (args[0] != null && args[1] != null) {
			File file = new File(args[0]);
				try {
					// Put file bytes into fileData byte array
					byte[] fileData = new byte[(int) file.length()];
					FileInputStream in = new FileInputStream(file);
					in.read(fileData);
					in.close();
					
					createAudioFile(fileData, args[1], 1, 1, 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
		} else {
			throw new IllegalArgumentException("You must input a file");
		}
	}
}
