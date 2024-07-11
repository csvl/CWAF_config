package be.uclouvain.model;

public class VirtualHost {

    private String name;
    private int port;

    public VirtualHost(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        String[] parts = path.split(":");
        this.name = parts[0];
        if (parts.length > 1) {
            this.port = Integer.parseInt(parts[1]);
        }
    }

    public VirtualHost(String path, int port) {
        this.name = path;
        this.port = port;
    }

    public boolean match(String host, int port) {
        if (this.name == null) {
            return true;
        }
        VirtualHost other = new VirtualHost(host, port);
        return this.equals(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VirtualHost other = (VirtualHost) obj;
        return (name != null ? (name.equals(other.name) || name.equals("*")) : other.name == null) &&
                port == other.port;
    }
    
}
