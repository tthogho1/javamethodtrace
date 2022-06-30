import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class StreamRedirecter extends Thread {
	private static final int BUFFER_SIZE = 2048;
	private final Reader in;
	private final Writer out;

	public StreamRedirecter(String name, InputStream in, OutputStream out) {
		super(name);
		this.in = new InputStreamReader(in); // stream to copy from
		this.out = new OutputStreamWriter(out); // stream to copy to
		setPriority(Thread.MAX_PRIORITY - 1);
	} // end of StreamRedirecter()

	public void run()
	// copy BUFFER_SIZE chars at a time
	{
		try {
			char[] cbuf = new char[BUFFER_SIZE];
			int count;
			while ((count = in.read(cbuf, 0, BUFFER_SIZE)) >= 0)
				out.write(cbuf, 0, count);
			out.flush();
		} catch (IOException e) {
			System.err.println("StreamRedirecter: " + e);
		}
	} // end of run()
}