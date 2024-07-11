package be.uclouvain.service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.stream.Stream;

import be.uclouvain.model.Directive;
import static be.uclouvain.utils.OntUtils.*;

public class Filter {

        public static int getDefaultPort(String scheme) {
            switch (scheme) {
                case "http":
                    return 80;
                case "https":
                    return 443;
                default:
                    return -1;
            }
        }

        public static Stream<Directive> filterFromRequest(Stream<Directive> order, HttpRequest request) {

        URI uri = request.uri();
        String path = uri.getPath();
        int port_tmp = uri.getPort();
        if (port_tmp == -1) {
            port_tmp = getDefaultPort(uri.getScheme());
        }
        final int port = port_tmp;
        
        return order.filter(d -> {
            return d.getLocation().match(path) && d.getVirtualHost().match(uri.getHost(), port);
        });
    }
    
    public static void main(String[] args) {

        Stream<Directive> order = readStreamFromFile("global_order.ser");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost/test/special/data"))
                .build();

        Stream<Directive> filtered = filterFromRequest(order, request);
        filtered.forEach(System.out::println);
        // order.forEach(System.out::println);
    }
}
