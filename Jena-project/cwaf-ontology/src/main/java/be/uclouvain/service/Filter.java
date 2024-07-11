package be.uclouvain.service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.stream.Stream;

import be.uclouvain.model.Directive;
import static be.uclouvain.utils.OntUtils.*;

public class Filter {
        public static Stream<Directive> filterFromRequest(Stream<Directive> order, HttpRequest request) {

        URI uri = request.uri();
        String path = uri.getPath();
        
        return order.filter(d -> {
            boolean a = d.getLocation().match(path);
            boolean b = d.getVirtualHost().match(uri.getHost(), uri.getPort());
            return a && b;
        });
    }
    
    public static void main(String[] args) {

        Stream<Directive> order = readStreamFromFile("global_order.ser");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/test/toast/hi"))
                .build();

        Stream<Directive> filtered = filterFromRequest(order, request);
        filtered.forEach(System.out::println);
    }
}
