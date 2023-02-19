package audio;

import java.io.*;
import javax.sound.sampled.*;

/*
 * from 
 * https://www.docjava.com/book/cgij/doc/audio/AudioBuffer.java.html
 * 
 */
/********************************************************/
/**
 * An AudioBuffer is a self-describing fragment of audio in memory. It can be
 * read and written in a manner similar to java.io.ByteArrayInputStream and
 * java.io.ByteArrayOutputStream. It knows how to transcode itself from one
 * audio format to another.
 * <p>
 * GRJ - 11/5/02 - This code can probably be simplifed somewhat, but my initial
 * goal was to break out this functionality. The goal was to make an interface
 * that works. The implementation can be cleaned up in the next version.
 * 
 */
public class AudioBuffer {
	private AudioFormat format = null;
	private byte[] audioByteArray = null;
	private AudioInputStream inputStream = null;
	private ByteArrayOutputStream outputStream = null;

	/********************************************************/
	/**
	 * Construct an empty AudioBuffer which will hold data of the specified format.
	 */
	public AudioBuffer(AudioFormat format) {
		this.format = format;
	}

	/********************************************************/
	/**
	 * Construct an AudioBuffer containing audio from the supplied byte array.
	 */
	public AudioBuffer(AudioFormat format, byte[] audioByteArray) {
		this.format = format;
		this.audioByteArray = audioByteArray;
	}

	/********************************************************/
	/**
	 * Construct an AudioBuffer containing audio from the supplied AudioInputStream.
	 */
	public AudioBuffer(AudioInputStream ais) {
		try {
			audioByteArray = new byte[ais.available()];
			ais.mark(ais.available());
			ais.read(audioByteArray, 0, ais.available());
			format = ais.getFormat();
			ais.reset();
		} catch (IOException ioe) {
			// what should I do here?
			ioe.printStackTrace();
		}
	}

	/********************************************************/
	/**
	 * Get the AudioFormat.Encoding corresponding to the AudioBuffer's audio data.
	 */
	public AudioFormat.Encoding getEncoding() {
		return format.getEncoding();
	}

	/********************************************************/
	/**
	 * Get the sample rate (samples per second) of the AudioBuffer's data.
	 */
	public float getSampleRate() {
		return format.getSampleRate();
	}

	/********************************************************/
	/**
	 * Get the frame size (bytes per sample) of the AudioBuffer's data.
	 */
	public int getFrameSize() {
		return format.getFrameSize();
	}

	/********************************************************/
	/**
	 * Get the audio format of the buffer's data as an AudioFormat object.
	 */
	public AudioFormat getFormat() {
		return format;
	}

	/********************************************************/
	/**
	 * Obtain a byte array that contains the AudioBuffer's data.
	 */
	public byte[] toByteArray() {
		return audioByteArray;
	}

	/********************************************************/
	/**
	 * Obtain an AudioInputStream that contains the AudioBuffer's data.
	 */
	public AudioInputStream toAudioInputStream() {
		inputStream = new AudioInputStream(new ByteArrayInputStream(audioByteArray), format,
				audioByteArray.length / format.getFrameSize());
		return inputStream;
	}

	/********************************************************/
	/**
	 * Convert the encoding of the audio in the buffer to the specified encoding.
	 * <p>
	 * GRJ - 11/5/02 - This is inefficient code. See disclaimer above.
	 * <p>
	 * Also, there seems to be a bug in the implementation here (or in the
	 * transcoding version of AudioSystem.getAudioInputStream used by the
	 * implementation?). The available() method of AudioInputStream does not return
	 * a different length after the transcode if the number of bytes per sample
	 * changes. I had to do a workaround.
	 */
	public void transcode(AudioFormat.Encoding destinationEncoding) {
		if (destinationEncoding == null) {
			return;
		}

		try {
			AudioFormat formats[] = AudioSystem.getTargetFormats(destinationEncoding, format);

			int inputByteLength = audioByteArray.length;
			int outputByteLength = inputByteLength * formats[0].getFrameSize() / format.getFrameSize();

			AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(audioByteArray), format,
					inputByteLength / format.getFrameSize());

			ais = AudioSystem.getAudioInputStream(formats[0], ais);

			byte[] newByteArray = new byte[outputByteLength];
			ais.read(newByteArray, 0, outputByteLength);
			format = formats[0];
			ais.close();
			audioByteArray = newByteArray;
			inputStream = null;
		} catch (IOException ioe) {
			// what should I do here?
			ioe.printStackTrace();
		}

	}

	/********************************************************/
	/**
	 * Retrieve data from the buffer.
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		if (inputStream == null) {
			toAudioInputStream();
		}
		return inputStream.read(b, off, len);
	}

	/********************************************************/
	/**
	 * Add data to the buffer.
	 */
	public void write(byte[] b, int off, int len) {
		if (outputStream == null) {
			outputStream = new ByteArrayOutputStream();
		}
		outputStream.write(b, off, len);
		audioByteArray = outputStream.toByteArray();
	}
}