package Server;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SoundDataUtils {
  public static float[] load16BitPCMRawDataFileAsDoubleArray(File file) {
    InputStream in = null;
    if (file.isFile()) {
      long size = file.length();
      try {
        in = new FileInputStream(file);
        return readStreamAsDoubleArray(in, size);
      } catch (Exception e) {
      }
    }
    return null;
  }

  public static float[] readStreamAsDoubleArray(InputStream in, long size)
      throws IOException {
    int bufferSize = (int) (size / 2);
    float[] result = new float[bufferSize];
    DataInputStream is = new DataInputStream(in);
    for (int i = 0; i < bufferSize; i++) {
      result[i] = is.readShort() / 32768.0f;
    }
    return result;
  }

}
