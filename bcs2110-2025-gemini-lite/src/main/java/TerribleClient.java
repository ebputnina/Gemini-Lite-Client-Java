import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TerribleClient {
    public static void main(String[] args) throws Throwable {
        try (final var s = new Socket("demo.svc.leastfixedpoint.nl", 1958)) {
            final var i = s.getInputStream(); // used for reading byte based data, one byte at a time
            // do i set a variable that will be used to write the data to?
            final var o = s.getOutputStream(); // used for writing byte based data, raw bytes
            // then here i write it?
            o.write("gemini-lite://demo.svc.leastfixedpoint.nl/\r\n".getBytes());
            // \n Insert a newline in the text at this point.
            // \r Insert a carriage return in the text at this point.
            //When you write data to a stream, it is not written immediately, and it is buffered.
            // So use flush() when you need to be sure that all your data from buffer is written.
            o.flush();
            try (final var r = new BufferedReader(new InputStreamReader(i))) { // used for reading line based data
                final var rep = r.readLine(); // read a line
                if (rep.startsWith("2")) { // if the first line starts with "2"
                    try (final var w = new PrintWriter(System.out)) {
                        r.transferTo(w); // transfer the rest of the data
                    }
                } else {
                    System.err.println(rep); // print the line to know what is wrong
                    System.exit(Integer.parseInt(rep.substring(0, 2)));
                }
            }
        }
    }
}
