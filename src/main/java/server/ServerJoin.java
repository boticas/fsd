package server;

import io.atomix.utils.net.Address;

/**
 * server.ServerJoin
 */
public class ServerJoin {
    private Address address;

    public ServerJoin(Address address) {
        this.address = address;
    }

    /**
     * @return the address
     */
    public Address getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        ServerJoin sj = (ServerJoin) obj;
        return this.address.equals(sj.address);
    }
}
