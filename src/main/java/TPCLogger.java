import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.SerializerBuilder;

/**
 * TPCLogger
 */
public class TPCLogger {
    private int numServers;

    // Coordinator
    private SegmentedJournal<CoordinatorLog> coordinatorSJ;
    private SegmentedJournalWriter<CoordinatorLog> coordinatorSJW;
    private HashMap<CoordinatorLog, AtomicInteger> ongoingCoordinatorTPC;

    // Server
    private SegmentedJournal<ServerLog> serverSJ;
    private SegmentedJournalWriter<ServerLog> serverSJW;
    private HashSet<ServerLog> ongoingTPC;

    public TPCLogger(int numServers, int port) {
        this.numServers = numServers;

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
        this.ongoingTPC = new HashSet<>();

        this.applyPendingLog();
    }

    // NOT IMPLEMENTED
    private void applyPendingLog() {
        SegmentedJournalReader<CoordinatorLog> coordinatorSJR = this.coordinatorSJ.openReader(0);
        while (coordinatorSJR.hasNext()) {
            Indexed<CoordinatorLog> l = coordinatorSJR.next();
            System.out.println(l.index() + ": " + l.entry());
        }
        coordinatorSJR.close();

        SegmentedJournalReader<ServerLog> serverSJR = serverSJ.openReader(0);
        while (serverSJR.hasNext()) {
            Indexed<ServerLog> l = serverSJR.next();
            System.out.println(l.index() + ": " + l.entry());
        }
        serverSJR.close();
    }

    /**
     * @param tpc
     * @param status
     * @return true if all servers have confirmed the transaction
     */
    public boolean updateCoordinatorLog(TwoPhaseCommit tpc, CoordinatorLog.Status status) {
        CoordinatorLog log = new CoordinatorLog();
        log.setTpc(tpc);
        log.setStatus(status);

        synchronized (this.coordinatorSJW) {
            coordinatorSJW.append(log);
            coordinatorSJW.flush();
        }

        synchronized (this.ongoingCoordinatorTPC) {
            if (status == CoordinatorLog.Status.STARTED) {
                ongoingCoordinatorTPC.put(log, new AtomicInteger(0));
                return false;
            } else { // status == (CoordinatorLog.Status.COMMITED || CoordinatorLog.Status.ABORTED)
                int numAccepted = ongoingCoordinatorTPC.get(log).incrementAndGet();
                if (numAccepted == this.numServers) {
                    ongoingCoordinatorTPC.remove(log);
                    return true;
                } else
                    return false;
            }
        }
    }

    /**
     * @param tpc
     * @param status
     * @return true if all servers have confirmed the transaction
     */
    public void updateServerLog(TwoPhaseCommit tpc, ServerLog.Status status) {
        ServerLog log = new ServerLog();
        log.setTpc(tpc);
        log.setStatus(status);

        synchronized (this.serverSJW) {
            serverSJW.append(log);
            serverSJW.flush();
        }

        synchronized (this.ongoingTPC) {
            if (status == ServerLog.Status.PREPARED)
                ongoingTPC.add(log);
            else // status == (ServerLog.Status.COMMITED || ServerLog.Status.ABORTED)
                ongoingTPC.remove(log);
        }
    }
}
