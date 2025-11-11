package net.pascalhp.webprobe.checker.checkers.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static net.pascalhp.webprobe.Application.app;

public class Request {
    private boolean stopped = false;
    private Socket sock;
    public URL url;
    public Headers headers = new Headers();
    public String full;
    public long durationMs;

    public Request(Socket sock, URL url) {
        this.sock = sock;
        this.url = url;
        this.headers.setHeader("Host", url.getHost());
        this.headers.setHeader("Connection", "close");
        this.headers.setHeader("User-Agent", "Webprobe/" + app.getVersion().getReadable());
    }

    public Response execute() throws Exception {
        InputStream inStream = sock.getInputStream();
        OutputStream outSteam = sock.getOutputStream();

        OutputStreamWriter out = new OutputStreamWriter(outSteam, StandardCharsets.UTF_8);

        StringBuilder req = new StringBuilder();
        req.append("GET ");
        req.append(getURI(this.url));
        req.append(" HTTP/1.1\r\n");

        this.headers.forEach((name, values) -> {
            for (String value : values) {
                req.append(Headers.normalizeHeader(name));
                req.append(": ");
                req.append(value);
                req.append("\r\n");
            }
        });

        req.append("\r\n");

        long start = System.currentTimeMillis();
        this.full = req.toString();
        out.write(this.full);
        out.flush();
        this.durationMs = System.currentTimeMillis() - start;

        InputStreamReader inReader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
        //BufferedReader linesReader = new BufferedReader(new InputStreamReader(inStream));

        Response resp = new Response();
        resp.headers = new Headers();

        String welcomeString = null;

        // TODO: rewrite response read. Limit response body size.
        start = System.currentTimeMillis();
        byte[] binLine;
        String line;
        while (true) {
            binLine = readLine(inStream);
            if (binLine == null || binLine.length == 0) {
                break;
            }
            line = new String(binLine, StandardCharsets.UTF_8).trim();
            if (line.isEmpty()) {
                break;
            }
            if (welcomeString == null) {
                welcomeString = line;
            } else {
                resp.headers.addRawHeader(line);
            }
        }
        resp.headersTimeMs = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        try {
            resp.body = inStream.readAllBytes();
        } catch (Throwable e) {
            resp.body = new byte[] {};
        }
        resp.bodyTimeMs = System.currentTimeMillis() - start;

        if (welcomeString != null) {
            String[] pieces = welcomeString.split("\\s+", 3);
            resp.protocol = pieces[0];
            resp.code = Integer.parseInt(pieces.length > 1 ? pieces[1] : "0");
            resp.message = pieces.length > 2 ? pieces[2] : "";
            resp.welcomeString = welcomeString;
        }

        inStream.close();
        outSteam.close();
        this.sock.close();

        resp.contentType = resp.headers.getOneHeader("Content-Type");

        if (welcomeString == null) {
            throw new Exception("The server didn't send a welcome string");
        }

        if (resp.code < 1) {
            throw new Exception("The welcome string does not contain a response code");
        }

        return resp;
    }

    protected static byte[] readLine(InputStream stream) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        int b;
        try {
            while (true) {
                b = stream.read();
                if (b < 0) {
                    break;
                }
                buf.write(b);
                if (b == '\n') {
                    break;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return buf.toByteArray();
    }

    protected static String getURI(URL url) {
        String result = url.getPath();
        String query = url.getQuery();
        if (query != null) {
            result += "?";
            result += query;
        }
        return result;
    }

    public void stop() {
        if (!this.stopped) {
            this.stopped = true;
            try {
                this.sock.close();
            } catch (Throwable e) {
            }
        }
    }
}
