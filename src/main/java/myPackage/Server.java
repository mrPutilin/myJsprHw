package myPackage;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
                final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        ) {

            final int limit = 4096;

            in.mark(limit);

            byte[] buffer = new byte[limit];
            final var read = in.read(buffer);
            byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
            int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                send404(out);
                return;
            }
            final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                send404(out);
                return;
            }

            String method = requestLine[0];

            String[] mainPathAndQuery = requestLine[1].split("\\?");

            List<NameValuePair> listOfQuery = URLEncodedUtils.parse(new URI(requestLine[1]), StandardCharsets.UTF_8);

            int headersStart = requestLineEnd + requestLineDelimiter.length;
            byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            int requestHeadersEnd = indexOf(buffer, headersDelimiter, headersStart, read);

            in.reset();

            in.skip(headersStart);

            byte[] headersBytes = in.readNBytes(requestHeadersEnd - headersStart);
            List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

            List<NameValuePair> bodies = new ArrayList<>();

            if (!method.equals("GET")) {
                in.skip(headersDelimiter.length);

                Optional<String> contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    int length = Integer.parseInt(contentLength.get());
                    byte[] bodBytes = in.readNBytes(length);
                    bodies = URLEncodedUtils.parse(new String(bodBytes), StandardCharsets.UTF_8);
                }
            }
            System.out.println(bodies);

            Request request = new Request(requestLine[0], requestLine[1], requestLine[2], headers, listOfQuery, bodies);
            System.out.println(request);


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


            final String path = requestLine[1];
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

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
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
