package server;

/**
 * server.Status
 */
public class Status {
    private TwoPhaseCommit tpc;
    private Log.Status status;
    
    public Status(TwoPhaseCommit tpc, Log.Status status) {
        this.tpc = tpc;
        this.status = status;
    }

    /**
     * @return the tpc
     */
    public TwoPhaseCommit getTpc() {
        return tpc;
    }

    /**
     * @return the status status
     */
    public Log.Status getStatus() {
        return status;
    }
}
