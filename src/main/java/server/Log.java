package server;

/**
 * server.Log
 */
public class Log {
    public enum Status {
        PREPARE, COMMIT, ABORT, HEARTBEAT
    }

    private TwoPhaseCommit tpc;
    private Status status;

    public Log(TwoPhaseCommit tpc, Log.Status status) {
        this.tpc = tpc;
        this.status = status;
    }

    /**
     * Get - tpc.
     * 
     * @return tpc
     */
    public TwoPhaseCommit getTpc() {
        return tpc;
    }

    /**
     * Get - status.
     * 
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Method hashCode.
     * 
     * @return int
     */
    @Override
    public int hashCode() {
        return tpc.getCount();
    }

    /**
     * Method equals.
     * 
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        Log cl = (Log) obj;
        return this.tpc.equals(cl.tpc); // Ignore the status
    }
}
