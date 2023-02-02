package myPackage;

import org.apache.http.NameValuePair;

import java.util.List;

public class Request {
    private final String methodRequest;
    private final List<String> headerRequest;
    private final String protocol;
    private final String path;
    private final List<NameValuePair> queryParams;
    private final List<NameValuePair> postBodyParams;



    public Request(String methodRequest, String path, String protocol, List<String> headerRequest,
                   List<NameValuePair> params, List<NameValuePair> postBodyParams) {
        this.methodRequest = methodRequest;
        this.path = path;
        this.protocol = protocol;
        this.headerRequest = headerRequest;
        this.queryParams = params;
        this.postBodyParams = postBodyParams;


    }

    public String getMethodRequest() {
        return methodRequest;
    }

    public List<String> getHeaderRequest() {
        return headerRequest;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public List<NameValuePair> getBodyParams() { return postBodyParams;}

    @Override
    public String toString() {
        return "Request{" +
                "methodRequest='" + methodRequest + '\'' +
                ", headerRequest=" + headerRequest +
                ", protocol='" + protocol + '\'' +
                ", path='" + path + '\'' +
                ", queryParams=" + queryParams +
                ", postBodyParams=" + postBodyParams +
                '}';
    }
}
