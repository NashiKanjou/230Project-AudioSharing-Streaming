package audio;

import java.io.*;
import javax.sound.sampled.*;
import javax.swing.event.*;

/********************************************************/
/**
 * An AudioPlayer knows how to access audio hardware to capture and playback
 * sound. It also knows how to access the file system to read and write audio
 * files. It can read and write audio files in multiple formats and with
 * multiple audio data encodings.
 */
public class AudioPlayer {
	private boolean isCapturing = false;
	private boolean isPlaying = false;
	private AudioBuffer audioBuffer;
	private String audioDescription;
	private EventListenerList listenerList = new EventListenerList();

	private static final AudioFormat defaultFormat = new AudioFormat(8000, // sample rate
			16, // sampleSizeInBits
			1, // channels
			true, // signed
			true); // big endian

	/********************************************************/
	/**
	 * Record audio into the internal buffer. This is a time-consuming operation.
	 * The calling program is responsible for running this operation in a separate
	 * thread.
	 */
	public AudioPlayer(AudioBuffer audioBuffer) {
		audioDescription = "streaming";
		this.audioBuffer = audioBuffer;
	}

	public void captureAudio() throws AudioPlayerException {
		int bufferSize;
		byte buffer[];
		TargetDataLine line = null;
		String errorString = null;

		// As this class only maintains one array of recorded data,
		// multiple instances of capture cannot be occuring simultaneously.
		// Play and record share both share this same array, so it is also
		// not possible to play and record at the same time.
		if (isCapturing) {
			throw new AudioPlayerException(" Multiple simultaneous captures not allowed.");
		}
		if (isPlaying) {
			throw new AudioPlayerException(" Simultaneous play and record not allowed.");
		}

		audioBuffer = new AudioBuffer(defaultFormat);

		// set buffer size to 10ms for low latency
		bufferSize = ((int) (0.01 * defaultFormat.getSampleRate())) * defaultFormat.getFrameSize();
		buffer = new byte[bufferSize];

		// Notify listeners that capture is starting.
		fireStateChangedEvent(State.RECORDING);

		// Begin capturing.
		isCapturing = true;
		try {
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, defaultFormat);
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(defaultFormat);
			line.start();

			while (isCapturing) {
				int count = line.read(buffer, 0, buffer.length);
				if (count > 0) {
					audioBuffer.write(buffer, 0, count);
				}
			}
			audioDescription = "Captured audio";

		} catch (LineUnavailableException e) {
			errorString = " Can't get an input line in captureAudio: " + e.getMessage();
		} finally {
			if (line != null) {
				line.stop();
				line.close();
			}
			isCapturing = false;

			// Notify listeners that capture has stopped.
			fireStateChangedEvent(State.IDLE);

			if (errorString != null) {
				throw new AudioPlayerException(errorString);
			}
		}
	}

	/********************************************************/
	/**
	 * Read audio from a file to the internal buffer. This is a time-consuming
	 * operation. The calling program is responsible for running this operation in a
	 * separate thread.
	 */
	public void openAudioFile(File readFile) throws AudioPlayerException {

		if (isCapturing || isPlaying) {
			throw new AudioPlayerException(" Please stop audio before opening new file.");
		}

		fireStateChangedEvent(State.OPENING);
		try {
			audioBuffer = new AudioBuffer(AudioSystem.getAudioInputStream(readFile));
			audioDescription = readFile.getName();

			fireStateChangedEvent(State.IDLE);
		} catch (UnsupportedAudioFileException a) {
			fireStateChangedEvent(State.IDLE);
			throw new AudioPlayerException(" Cannot read " + readFile.getName());
		} catch (IOException e) {
			fireStateChangedEvent(State.IDLE);
			throw new AudioPlayerException(" Error reading " + readFile.getName());
		}
	}

	/********************************************************/
	/**
	 * Play audio in the internal buffer. This is a time-consuming operation. The
	 * calling program is responsible for running this operation in a separate
	 * thread.
	 */
	public void playAudio() throws AudioPlayerException {
		byte buffer[];
		int bufferSize;
		SourceDataLine line = null;
		String errorString = null;

		// As play and record share both share the same audio data array,
		// it is not possible to play and record at the same time.
		if (isCapturing) {
			throw new AudioPlayerException(" Simultaneous play and record not allowed.");
		}

		if (audioBuffer == null) {
			throw new AudioPlayerException(" No audio to play.");
		}

		// The SourceDataLine requires linear data for playback through
		// the audio hardware. Transcode the audioBuffer's data to
		// linear.

		audioBuffer.transcode(AudioFormat.Encoding.PCM_SIGNED);

		// Notify listeners that playing is starting.
		fireStateChangedEvent(State.PLAYING);

		// Begin playing
		isPlaying = true;
		try {
			// set buffer size to 10ms for low latency
			bufferSize = ((int) (0.01 * audioBuffer.getSampleRate())) * audioBuffer.getFrameSize();
			buffer = new byte[bufferSize];

			DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioBuffer.getFormat());
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioBuffer.getFormat());
			line.start();

			while (isPlaying) {
				int count;

				count = audioBuffer.read(buffer, 0, buffer.length);

				if (count < 0) {
					isPlaying = false;
				} else if (count > 0) {
					line.write(buffer, 0, count);
				}
			}
		} catch (IllegalArgumentException e) {
			errorString = " Unplayable audio format \n" + e.getMessage();
		} catch (LineUnavailableException e) {
			errorString = " Can't get an output line in playAudio: " + e.getMessage();
		} catch (IOException e) {
			errorString = " Audio output problem: " + e.getMessage();
		} finally {
			if (line != null) {
				line.drain();
				line.close();
			}
			isPlaying = false;

			// Notify listeners that playing has stopped.
			fireStateChangedEvent(State.IDLE);

			if (errorString != null) {
				throw new AudioPlayerException(errorString);
			}
		}
	}

	/********************************************************/
	/**
	 * Save audio in the internal buffer to a file after converting to the specified
	 * encoding. This is a time-consuming operation. The calling program is
	 * responsible for running this operation in a separate thread.
	 */
	public void saveAudioFile(File writeFile, AudioFormat.Encoding writeEncoding) throws AudioPlayerException {

		if (isCapturing || isPlaying) {
			throw new AudioPlayerException(" Please stop audio before saving.");
		}

		if (audioBuffer == null) {
			throw new AudioPlayerException(" Nothing to save.");
		}

		fireStateChangedEvent(State.SAVING);

		try {
			audioBuffer.transcode(writeEncoding);

			// save
			FileOutputStream fos = new FileOutputStream(writeFile);

			AudioFileFormat.Type type = AudioFileFormat.Type.AU;
			if (writeFile.getName().endsWith(".wav")) {
				type = AudioFileFormat.Type.WAVE;
			}

			AudioSystem.write(audioBuffer.toAudioInputStream(), type, fos);
			fos.close();
			audioDescription = writeFile.getName();
			fireStateChangedEvent(State.IDLE);

		} catch (IOException ioe) {
			fireStateChangedEvent(State.IDLE);
			throw new AudioPlayerException(" No file created");
		}
	}

	/********************************************************/
	/**
	 * Save audio in the internal buffer to a file. This is a time-consuming
	 * operation. The calling program is responsible for running this operation in a
	 * separate thread.
	 */
	public void saveAudioFile(File writeFile) throws AudioPlayerException {
		saveAudioFile(writeFile, null);
	}

	/********************************************************/
	/**
	 * Return an AudioFormat object specifying the type of audio dealt with by this
	 * class.
	 */
	public AudioFormat getFormat() {
		return audioBuffer.getFormat();
	}

	/********************************************************/
	/**
	 * Return a String describing the audio currently stored in the internal buffer
	 * (file name or other source).
	 */
	public String getAudioDescription() {
		return audioDescription;
	}

	/********************************************************/
	/**
	 * Stop recording or playing.
	 */
	public void stop() {
		if (isCapturing == true || isPlaying == true) {
			isCapturing = false;
			isPlaying = false;
			fireStateChangedEvent(State.STOPPING);
		}
	}

	/********************************************************/
	/**
	 * Inner class to provide typesafe enumerated audio states.
	 */
	public static class State {
		private String desc;

		public static final State IDLE = new State("idle");
		public static final State STOPPING = new State("stopping");
		public static final State RECORDING = new State("recording");
		public static final State PLAYING = new State("playing");
		public static final State OPENING = new State("opening");
		public static final State SAVING = new State("saving");

		private State(String s) {
			desc = s;
		}

		public String toString() {
			return desc;
		}
	}

	/********************************************************/
	/**
	 * Allows other classes to listen for AudioPlayer events
	 */
	public void addAudioPlayerListener(AudioPlayerListener o) {
		listenerList.add(AudioPlayerListener.class, o);
		o.audioStateChanged(State.IDLE);
	}

	/********************************************************/
	/**
	 * Allows other classes to stop listening to AudioPlayer events
	 */

	public void removeAudioPlayerListener(AudioPlayerListener o) {
		listenerList.remove(AudioPlayerListener.class, o);
	}

	/********************************************************/
	/**
	 * Internally used for notifying listeners of events
	 */

	private void fireStateChangedEvent(State newState) {
		Object listeners[] = listenerList.getListeners(AudioPlayerListener.class);

		for (int i = 0; i < listeners.length; i++) {
			((AudioPlayerListener) listeners[i]).audioStateChanged(newState);
		}
	}
}