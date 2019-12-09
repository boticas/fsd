import java.util.ArrayList;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

class HeartbeatThread extends Thread {
    ArrayList<Address> servers;
    Serializer serializer;
    ManagedMessagingService ms;
    TPCHandler tpcHandler;

    public HeartbeatThread(ArrayList<Address> servers, Serializer serializer, ManagedMessagingService ms,
            TPCHandler tpcHandler) {
        this.servers = servers;
        this.serializer = serializer;
        this.ms = ms;
        this.tpcHandler = tpcHandler;
    }

    public void run() {
        while (!this.ms.isRunning()) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                return;
            }
        }

        while (true) {
            TwoPhaseCommit heartbeat = new TwoPhaseCommit(this.tpcHandler.getAndIncrementTotalOrderCounter());
            for (Address address : this.servers)
                this.ms.sendAsync(address, "tpcHeartbeat", serializer.encode(heartbeat));

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                return;
            }
        }
    }
}
