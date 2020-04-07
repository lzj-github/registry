package cn.lzj.nacos.naming.cluster;

import lombok.Data;

@Data
public class Server {

    private String ip;

    private int servePort;

    private int weight = 1;

    private boolean alive = false;

    private long lastRefTime = 0L;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Server server = (Server) o;
        return servePort == server.servePort && ip.equals(server.ip);
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + servePort;
        return result;
    }
}
