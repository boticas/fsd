import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.util.Pair;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

/**
 * TPCHandler
 */
public class TPCHandler {
    private enum TPCStatus {
        ONGOING, COMMITED, ABORTED
    }

    private ArrayList<Address> servers;
    private Serializer serializer;
    private ManagedMessagingService ms;

    private Integer totalOrderCounter;

    // Coordinator
    private SegmentedJournal<Log> coordinatorSJ;
    private SegmentedJournalWriter<Log> coordinatorSJW;
    private HashMap<Log, HashSet<Address>> coordinatorTPCs;

    // Server
    private SegmentedJournal<Log> serverSJ;
    private SegmentedJournalWriter<Log> serverSJW;

    private HashMap<Address, TreeMap<Integer, Pair<TPCStatus, TwoPhaseCommit>>> pendingTransactions;

    public TPCHandler(ArrayList<Address> servers, int port, Serializer serializer, ManagedMessagingService ms) {
        this.servers = servers;
        this.serializer = serializer;
        this.ms = ms;

        this.coordinatorSJ = SegmentedJournal.<Log>builder().withName("coordinatorLog-" + port)
                .withSerializer(new SerializerBuilder().addType(Tweet.class).addType(TwoPhaseCommit.class)
                        .addType(Log.class).addType(Log.Status.class).addType(Address.class).build())
                .build();
        this.coordinatorSJW = coordinatorSJ.writer();
        this.coordinatorTPCs = new HashMap<>();

        this.totalOrderCounter = 0;

        this.serverSJ = SegmentedJournal.<Log>builder().withName("serverLog-" + port)
                .withSerializer(new SerializerBuilder().addType(Tweet.class).addType(TwoPhaseCommit.class)
                        .addType(Log.class).addType(Log.Status.class).addType(Address.class).build())
                .build();
        this.serverSJW = serverSJ.writer();

        this.pendingTransactions = new HashMap<>();
        for (int i = 0; i < this.servers.size(); i++)
            this.pendingTransactions.put(this.servers.get(i), new TreeMap<>());
    }

    public void applyPendingLog() {
        ;
    }

    /**
     * @return the totalOrderCounter
     */
    public Integer getAndIncrementTotalOrderCounter() {
        synchronized (this.totalOrderCounter) {
            int res = this.totalOrderCounter;
            this.totalOrderCounter++;
            return res;
        }
    }

    /**
     * @param log
     */
    private void writeCoordinatorLog(Log log) {
        synchronized (this.coordinatorSJW) {
            this.coordinatorSJW.append(log);
            this.coordinatorSJW.flush();
        }
    }

    /**
     * @param tpc
     * @param status
     * @param remote
     * @return true if all servers have confirmed the transaction
     */
    public boolean updateCoordinatorLog(TwoPhaseCommit tpc, Log.Status status, Address remote) {
        Log log = new Log(tpc, status);

        synchronized (this.coordinatorTPCs) {
            if (status == Log.Status.PREPARE) {
                this.writeCoordinatorLog(log);
                this.coordinatorTPCs.put(log, new HashSet<>(this.servers.size()));
                return false;
            } else { // status == (Log.Status.COMMIT || Log.Status.ABORT)
                HashSet<Address> accepted = coordinatorTPCs.get(log);
                accepted.add(remote);
                if (accepted.size() == this.servers.size()) {
                    this.writeCoordinatorLog(log);
                    return true;
                } else
                    return false;
            }
        }
    }

    /**
     * @param log
     */
    private void writeServerLog(Log log) {
        synchronized (this.serverSJW) {
            this.serverSJW.append(log);
            this.serverSJW.flush();
        }
    }

    /**
     * @param tpc
     * @param status
     * @param remote
     * @return the tpcs that should be applied next
     */
    public ArrayList<TwoPhaseCommit> updateServerLog(TwoPhaseCommit tpc, Log.Status status, Address remote) {
        Log log = new Log(tpc, status);

        this.writeServerLog(log);

        if (status == Log.Status.PREPARE) {
            synchronized (this.pendingTransactions) {
                this.pendingTransactions.get(remote).put(tpc.getCount(), new Pair<>(TPCStatus.ONGOING, tpc));
            }

            synchronized (this.totalOrderCounter) {
                if (tpc.getCount() >= this.totalOrderCounter)
                    this.totalOrderCounter = tpc.getCount() + 1;
            }

            return null;
        } else { // status == (Log.Status.COMMIT || Log.Status.ABORT || Log.Status.HEARTBEAT)
            synchronized (this.pendingTransactions) {
                if (tpc.isHeartbeat()) {
                    synchronized (this.totalOrderCounter) {
                        this.pendingTransactions.get(remote).put(tpc.getCount(), new Pair<>(TPCStatus.ONGOING, tpc));
                        if (tpc.getCount() >= this.totalOrderCounter)
                            this.totalOrderCounter = tpc.getCount() + 1;
                    }
                } else {
                    if (status == Log.Status.COMMIT)
                        this.pendingTransactions.get(remote).put(tpc.getCount(), new Pair<>(TPCStatus.COMMITED, tpc));
                    else
                        this.pendingTransactions.get(remote).put(tpc.getCount(), new Pair<>(TPCStatus.ABORTED, tpc));
                }

                ArrayList<TwoPhaseCommit> tpcsToApply = new ArrayList<>();
                while (true) {
                    // Find the message with the lower count or stop if there isn't at least one
                    // from each server
                    int sCount = Integer.MAX_VALUE;
                    Address sAddress = new Address("255.255.255.255", 65535);
                    ArrayList<Address> emptyAddresses = new ArrayList<>();
                    for (Address a : this.pendingTransactions.keySet()) {
                        TreeMap<Integer, Pair<TPCStatus, TwoPhaseCommit>> pt = this.pendingTransactions.get(a);
                        if (pt.size() == 0) {
                            if (tpc.isHeartbeat())
                                return tpcsToApply;

                            emptyAddresses.add(a);
                            continue;
                        }

                        if (pt.firstKey() < sCount) {
                            sCount = pt.firstKey();
                            sAddress = a;
                        } else if (pt.firstKey() == sCount) {
                            if (a.toString().compareTo(sAddress.toString()) < 0)
                                sAddress = a;
                        }
                    }

                    if (!tpc.isHeartbeat()) {
                        if (emptyAddresses.size() == this.servers.size())
                            return tpcsToApply;

                        if (emptyAddresses.size() > 0) {
                            for (Address address : emptyAddresses)
                                this.ms.sendAsync(address, "tpcGetHeartbeat", serializer.encode(null));

                            return tpcsToApply;
                        }
                    }

                    // Check if the smallest transaction is still being processed or is an heartbeat
                    Map.Entry<Integer, Pair<TPCStatus, TwoPhaseCommit>> toProcess = this.pendingTransactions
                            .get(sAddress).pollFirstEntry();
                    Pair<TPCStatus, TwoPhaseCommit> tpcToProcess = toProcess.getValue();
                    if (tpcToProcess.getValue().isHeartbeat())
                        continue;

                    if (tpcToProcess.getKey() == TPCStatus.ONGOING) {
                        this.pendingTransactions.get(sAddress).put(toProcess.getKey(), tpcToProcess);
                        return tpcsToApply;
                    } else if (tpcToProcess.getKey() == TPCStatus.COMMITED)
                        tpcsToApply.add(tpcToProcess.getValue());
                }
            }
        }
    }

    /**
     * @param heartbeat
     * @param remote
     * @return the tpcs that should be applied next
     */
    public ArrayList<TwoPhaseCommit> processHeartbeat(TwoPhaseCommit heartbeat, Address remote) {
        return this.updateServerLog(heartbeat, Log.Status.HEARTBEAT, remote);
    }
}
