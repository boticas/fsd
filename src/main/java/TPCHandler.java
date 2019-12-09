import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.SerializerBuilder;

/**
 * TPCHandler
 */
public class TPCHandler {
    private ArrayList<Address> servers;

    private Integer totalOrderCounter;
    private HashMap<Address, TreeMap<Integer, TwoPhaseCommit>> pendingTransactions;

    // Coordinator
    private SegmentedJournal<CoordinatorLog> coordinatorSJ;
    private SegmentedJournalWriter<CoordinatorLog> coordinatorSJW;
    private HashMap<CoordinatorLog, AtomicInteger> ongoingCoordinatorTPC;

    // Server
    private SegmentedJournal<ServerLog> serverSJ;
    private SegmentedJournalWriter<ServerLog> serverSJW;

    public TPCHandler(ArrayList<Address> servers, int port) {
        this.servers = servers;

        this.totalOrderCounter = 0;
        this.pendingTransactions = new HashMap<>();
        for (int i = 0; i < this.servers.size(); i++)
            this.pendingTransactions.put(this.servers.get(i), new TreeMap<>());

        this.coordinatorSJ = SegmentedJournal.<CoordinatorLog>builder().withName("coordinatorLog-" + port)
                .withSerializer(new SerializerBuilder().addType(Tweet.class).addType(TwoPhaseCommit.class)
                        .addType(CoordinatorLog.class).addType(CoordinatorLog.Status.class).build())
                .build();
        this.coordinatorSJW = coordinatorSJ.writer();
        this.ongoingCoordinatorTPC = new HashMap<>();

        this.serverSJ = SegmentedJournal.<ServerLog>builder().withName("serverLog-" + port)
                .withSerializer(new SerializerBuilder().addType(Tweet.class).addType(TwoPhaseCommit.class)
                        .addType(ServerLog.class).addType(ServerLog.Status.class).build())
                .build();
        this.serverSJW = serverSJ.writer();

        this.applyPendingLog();
    }

    // NOT IMPLEMENTED
    private void applyPendingLog() {
        return;
        /*
         * SegmentedJournalReader<CoordinatorLog> coordinatorSJR =
         * this.coordinatorSJ.openReader(0); while (coordinatorSJR.hasNext()) {
         * Indexed<CoordinatorLog> l = coordinatorSJR.next();
         * System.out.println(l.index() + ": " + l.entry()); } coordinatorSJR.close();
         * 
         * SegmentedJournalReader<ServerLog> serverSJR = serverSJ.openReader(0); while
         * (serverSJR.hasNext()) { Indexed<ServerLog> l = serverSJR.next();
         * System.out.println(l.index() + ": " + l.entry()); } serverSJR.close();
         */
    }

    /**
     * @param tpc
     * @param status
     * @return true if all servers have confirmed the transaction
     */
    public boolean updateCoordinatorLog(TwoPhaseCommit tpc, CoordinatorLog.Status status) {
        return updateCoordinatorLog(tpc, status, null);
    }

    /**
     * @param tpc
     * @param status
     * @param remote
     * @return true if all servers have confirmed the transaction
     */
    public boolean updateCoordinatorLog(TwoPhaseCommit tpc, CoordinatorLog.Status status, Address remote) {
        CoordinatorLog log = new CoordinatorLog();
        log.setTpc(tpc);
        log.setStatus(status);

        synchronized (this.coordinatorSJW) {
            coordinatorSJW.append(log);
            coordinatorSJW.flush();
        }

        synchronized (this.ongoingCoordinatorTPC) {
            if (status == CoordinatorLog.Status.STARTED) {
                synchronized (this.pendingTransactions) {
                    synchronized (this.totalOrderCounter) {
                        tpc.setCount(this.totalOrderCounter);
                        this.pendingTransactions.get(remote).put(this.totalOrderCounter, tpc);

                        this.totalOrderCounter++;
                    }
                }

                ongoingCoordinatorTPC.put(log, new AtomicInteger(0));
                return false;
            } else { // status == (CoordinatorLog.Status.COMMITED || CoordinatorLog.Status.ABORTED)
                int numAccepted = ongoingCoordinatorTPC.get(log).incrementAndGet();
                if (numAccepted == this.servers.size())
                    return true;
                else
                    return false;
            }
        }
    }

    /**
     * @param tpc
     * @param status
     * @return the tpc that can be applied next
     */
    public ArrayList<TwoPhaseCommit> updateServerLog(TwoPhaseCommit tpc, ServerLog.Status status) {
        return updateServerLog(tpc, status, null);
    }

    /**
     * @param tpc
     * @param status
     * @param remote
     * @return the tpc that can be applied next
     */
    public ArrayList<TwoPhaseCommit> updateServerLog(TwoPhaseCommit tpc, ServerLog.Status status, Address remote) {
        ServerLog log = new ServerLog();
        log.setTpc(tpc);
        log.setStatus(status);

        synchronized (this.serverSJW) {
            serverSJW.append(log);
            serverSJW.flush();
        }

        if (status == ServerLog.Status.PREPARED) {
            synchronized (this.pendingTransactions) {
                synchronized (this.totalOrderCounter) {
                    this.pendingTransactions.get(remote).put(tpc.getCount(), tpc);
                    if (tpc.getCount() >= this.totalOrderCounter)
                        this.totalOrderCounter = tpc.getCount() + 1;
                }
            }

            return null;
        } else { // status == (ServerLog.Status.COMMITED || ServerLog.Status.ABORTED)
            synchronized (this.pendingTransactions) {
                ArrayList<TwoPhaseCommit> tpcsToApply = new ArrayList<>();
                while (true) {
                    // Find the message with the lower count or stop if there isn't at least one
                    // from each server
                    int sCount = Integer.MAX_VALUE;
                    Address sAddress = new Address("255.255.255.255", 65535);
                    for (Address a : this.pendingTransactions.keySet()) {
                        TreeMap<Integer, TwoPhaseCommit> pt = this.pendingTransactions.get(a);
                        if (pt.size() == 0)
                            return tpcsToApply;

                        if (pt.firstKey() < sCount) {
                            sCount = pt.firstKey();
                            sAddress = a;
                        } else if (pt.firstKey() == sCount) {
                            if (a.toString().compareTo(sAddress.toString()) < 0)
                                sAddress = a;
                        }
                    }

                    // Remove the tpc from the map
                    TwoPhaseCommit toProcess = this.pendingTransactions.get(sAddress).pollFirstEntry().getValue();
                    tpcsToApply.add(toProcess);
                }
            }
        }
    }
}
