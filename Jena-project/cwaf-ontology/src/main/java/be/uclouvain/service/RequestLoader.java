package be.uclouvain.service;

import java.net.URI;
import java.net.http.HttpRequest;


public class RequestLoader {
    
    public static void main(String[] args) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:8000/very_important/file.txt"))
                .build();

        // Recovering the URI and Host header
        URI uri = request.uri();
        System.out.println("URI: " + uri);

        String hostHeader = request.headers().firstValue("Host").orElse(null);
        if (hostHeader == null) {
            // If Host header is not explicitly set, use the host from the URI
            hostHeader = uri.getHost();
        }
        System.out.println("Host: " + hostHeader);
    }
}
