package net.pascalhp.webprobe.checker.checkers.http;

public class Response {
    public String protocol;
    public int code;
    public String message;
    public String welcomeString;
    public Headers headers;
    public byte[] body;
    public long headersTimeMs;
    public long bodyTimeMs;
    public String contentType;
}
