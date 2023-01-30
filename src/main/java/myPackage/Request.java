package myPackage;

import org.apache.http.NameValuePair;

import java.util.List;

public class Request {
    private final String methodRequest;
    private final List<String> headerRequest;
    private final byte[] bodyRequest;
    private final String protocol;
    private final String path;

    private final List<NameValuePair> params;

    public Request(String methodRequest, String path, String protocol,
                   List<String> headerRequest, byte[] bodyRequest, List<NameValuePair> params) {
        this.methodRequest = methodRequest;
        this.path = path;
        this.protocol = protocol;
        this.headerRequest = headerRequest;
        this.bodyRequest = bodyRequest;
        this.params = params;


    }

    public String getMethodRequest() {
        return methodRequest;
    }

    public List<String> getHeaderRequest() {
        return headerRequest;
    }

    public byte[] getBodyRequest() {
        return bodyRequest;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQueryParams() {
        return params;
    }
}
