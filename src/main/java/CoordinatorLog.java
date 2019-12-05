/**
 * CoordinatorLog
 */
public class CoordinatorLog {
    public enum Status {
        STARTED, COMMITED, ABORTED
    }

    private TwoPhaseCommit tpc;
    private Status status;

    /**
     * @return the tpc
     */
    public TwoPhaseCommit getTpc() {
        return tpc;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param tpc the tpc to set
     */
    public void setTpc(TwoPhaseCommit tpc) {
        this.tpc = tpc;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }
}
