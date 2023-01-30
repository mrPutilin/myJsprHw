package myPackage;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();


    public void start() {

        final ExecutorService threadPool = Executors.newFixedThreadPool(64);

        try (final ServerSocket serverSocket = new ServerSocket(9999)) {

            while (true) {

                final Socket socket = serverSocket.accept();

                threadPool.submit(new ConnectionHandler(socket, handlers)::handle);
            }

        } catch (IOException w) {
            w.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method))
            handlers.put(method, new ConcurrentHashMap<>());

        handlers.get(method).put(path, handler);

    }

}

class ConnectionHandler {
    private final Socket socket;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers;
    private final List<String> validPath = List.of("default-get.html", "/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


    public ConnectionHandler(Socket socket, ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers) {
        this.socket = socket;
        this.handlers = handlers;

    }

    public void handle() {
        System.out.println(Thread.currentThread().getName());
        try (
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        ) {
            final String requestLine = in.readLine();
            final String[] parts = requestLine.split(" ");
            String[] mainPathAndQuery = parts[1].split("\\?");

            if (parts.length != 3) {
                return;
            }

            List<NameValuePair> listOfQuery = URLEncodedUtils.parse(new URI(parts[1]), StandardCharsets.UTF_8);

            Request request = new Request(parts[0], parts[1], parts[2], null, null, listOfQuery);

            if (!handlers.containsKey(request.getMethodRequest())) {
                send404(out);
                return;
            }

            ConcurrentHashMap<String, Handler> pathHandlers = handlers.get(request.getMethodRequest());

            if (!pathHandlers.containsKey(mainPathAndQuery[0])) {
                send404(out);
                return;
            }

            Handler handler = pathHandlers.get(mainPathAndQuery[0]);

            try {
                handler.handle(request, out);

            } catch (Exception e) {
                System.out.println(e.getMessage());
                send500(out);
            }


            final String path = parts[1];
            if (!validPath.contains(path)) {
                send404(out);
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
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }


    public void send404(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void send500(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 500 Internal Server Error\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

}
