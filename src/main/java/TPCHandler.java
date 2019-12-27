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
    private HashMap<Log, Pair<HashSet<Address>, TPCStatus>> coordinatorTPCs;

    // Server
    private SegmentedJournal<Log> serverSJ;
    private SegmentedJournalWriter<Log> serverSJW;

    private HashMap<Address, TreeMap<Integer, Pair<TPCStatus, TwoPhaseCommit>>> pendingTransactions;

    public TPCHandler(ArrayList<Address> servers, int port, Serializer serializer, ManagedMessagingService ms) {
        this.servers = servers;
        this.serializer = serializer;
        this.ms = ms;

        this.totalOrderCounter = 0;

        this.coordinatorSJ = SegmentedJournal.<Log>builder().withName("coordinatorLog-" + port)
                .withSerializer(new SerializerBuilder().addType(Tweet.class).addType(TwoPhaseCommit.class)
                        .addType(Log.class).addType(Log.Status.class).addType(Address.class).build())
                .build();
        this.coordinatorSJW = coordinatorSJ.writer();
        this.coordinatorTPCs = new HashMap<>();

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
        synchronized (this.totalOrderCounter) {
            synchronized (this.coordinatorTPCs) {
                // Coordinator
                SegmentedJournalReader<Log> coordinatorSJR = this.coordinatorSJ.openReader(0);
                while (coordinatorSJR.hasNext()) {
                    Log l = coordinatorSJR.next().entry();
                    if (l.getStatus() == Log.Status.PREPARE)
                        this.coordinatorTPCs.put(l, new Pair<>(new HashSet<>(this.servers.size()), TPCStatus.ONGOING));
                    else if (l.getStatus() == Log.Status.COMMIT)
                        this.coordinatorTPCs.put(l, new Pair<>(null, TPCStatus.COMMITED));
                    else if (l.getStatus() == Log.Status.ABORT)
                        this.coordinatorTPCs.put(l, new Pair<>(null, TPCStatus.ABORTED));

                    if (l.getTpc().getCount() >= this.totalOrderCounter)
                        this.totalOrderCounter = l.getTpc().getCount() + 1;
                }
                coordinatorSJR.close();

                for (Map.Entry<Log, Pair<HashSet<Address>, TPCStatus>> e : this.coordinatorTPCs.entrySet()) {
                    if (e.getValue().getValue() == TPCStatus.ONGOING) {
                        TwoPhaseCommit tpc = e.getKey().getTpc();
                        this.updateCoordinatorLog(tpc, Log.Status.PREPARE, null);
                        for (Address address : this.servers)
                            this.ms.sendAsync(address, "tpcPrepare", serializer.encode(tpc));
                    }
                }
            }

            synchronized (this.pendingTransactions) {
                // Server
                SegmentedJournalReader<Log> serverSJR = this.serverSJ.openReader(0);
                while (serverSJR.hasNext()) {
                    Log l = serverSJR.next().entry();
                    if (l.getStatus() == Log.Status.PREPARE)
                        this.pendingTransactions.get(l.getTpc().getCoordinator()).put(l.getTpc().getCount(),
                                new Pair<>(TPCStatus.ONGOING, l.getTpc()));
                    else if (l.getStatus() != Log.Status.HEARTBEAT)
                        this.pendingTransactions.get(l.getTpc().getCoordinator()).remove(l.getTpc().getCount());

                    if (l.getTpc().getCount() >= this.totalOrderCounter)
                        this.totalOrderCounter = l.getTpc().getCount() + 1;
                }
                serverSJR.close();

                for (Map.Entry<Address, TreeMap<Integer, Pair<TPCStatus, TwoPhaseCommit>>> t : this.pendingTransactions
                        .entrySet()) {
                    for (Pair<TPCStatus, TwoPhaseCommit> p : t.getValue().values()) {
                        TwoPhaseCommit tpc = p.getValue();
                        this.ms.sendAsync(t.getKey(), "tpcGetStatus", serializer.encode(tpc));
                    }

                    if (t.getValue().values().size() == 0)
                        this.ms.sendAsync(t.getKey(), "tpcGetStatus", serializer.encode(new TwoPhaseCommit(this.getAndIncrementTotalOrderCounter(), t.getKey())));
                }
            }
        }
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
     * @return the status of the tpc and if it is necessary to rollback
     */
    public Status getStatus(TwoPhaseCommit tpc, Address remote) {
        synchronized (this.coordinatorTPCs) {
            for (Map.Entry<Log, Pair<HashSet<Address>, TPCStatus>> e : this.coordinatorTPCs.entrySet()) {
                if (e.getValue().getValue() != TPCStatus.ONGOING || e.getKey().getTpc() == tpc)
                    continue;

                if (!e.getValue().getKey().contains(remote)) {
                    this.updateCoordinatorLog(e.getKey().getTpc(), Log.Status.ABORT, remote);
                    for (Address address : this.servers)
                        this.ms.sendAsync(address, "tpcRollback", serializer.encode(e.getKey().getTpc()));
                    Response result = new Response(false);
                    ms.sendAsync(e.getKey().getTpc().getRequester(), "result", serializer.encode(result));
                }
            }

            if (!tpc.isHeartbeat()) {
                Log log = new Log(tpc, Log.Status.PREPARE);
                Pair<HashSet<Address>, TPCStatus> accepted = this.coordinatorTPCs.get(log);
                if (accepted.getValue() == TPCStatus.ABORTED)
                    return new Status(tpc, Log.Status.ABORT);

                if (accepted.getValue() == TPCStatus.COMMITED)
                    return new Status(tpc, Log.Status.COMMIT);

                if (accepted.getKey().contains(remote))
                    return new Status(tpc, null);

                this.updateCoordinatorLog(tpc, Log.Status.ABORT, remote);
                for (Address address : this.servers)
                    ms.sendAsync(address, "tpcRollback", serializer.encode(tpc));
                Response result = new Response(false);
                ms.sendAsync(tpc.getRequester(), "result", serializer.encode(result));

                return new Status(tpc, null);
            } else
                return new Status(tpc, null);
        }
    }

    public ArrayList<TwoPhaseCommit> updateStatus(Status status) {
        return this.updateServerLog(status.getTpc(), status.getStatus(), status.getTpc().getCoordinator());
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
                this.coordinatorTPCs.put(log, new Pair<>(new HashSet<>(this.servers.size()), TPCStatus.ONGOING));
                return false;
            } else if (status == Log.Status.COMMIT) {
                Pair<HashSet<Address>, TPCStatus> accepted = coordinatorTPCs.get(log);
                accepted.getKey().add(remote);
                if (accepted.getKey().size() == this.servers.size()) {
                    this.writeCoordinatorLog(log);
                    this.coordinatorTPCs.put(log, new Pair<>(null, TPCStatus.COMMITED));
                    return true;
                } else
                    return false;
            } else { // status == Log.Status.ABORT
                this.writeCoordinatorLog(log);
                this.coordinatorTPCs.put(log, new Pair<>(null, TPCStatus.ABORTED));
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
