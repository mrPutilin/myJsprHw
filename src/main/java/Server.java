import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    static final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    public void start() {

        try (final ServerSocket serverSocket = new ServerSocket(9999)) {

            while (true) {

                final Socket socket = serverSocket.accept();

                threadPool.submit(new ThreadSocket(socket));
            }
        } catch (IOException w) {
            w.printStackTrace();
        }
        threadPool.shutdown();
    }
}

class ThreadSocket implements Runnable {
    Socket socket;
    List<String> validPath;

    public ThreadSocket(Socket socket) {
        this.socket = socket;
        this.validPath = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    }

    @Override
    public void run() {
        try (
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final String requestLine = in.readLine();
            final String[] parts = requestLine.split(" ");

            if (parts.length != 3) {
                return;
            }

            final String path = parts[1];
            if (!validPath.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final Path filePath = Path.of(".", "public", path);
            final String mimeType = Files.probeContentType(filePath);
            final long length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
