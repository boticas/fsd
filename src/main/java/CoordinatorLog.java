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

    @Override
    public int hashCode() {
        if (this.tpc.getTweet() != null)
            return (int) this.tpc.getTweet().getContent().charAt(0);
        else
            return (int) this.tpc.getUsername().charAt(0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        CoordinatorLog cl = (CoordinatorLog) obj;
        return this.tpc.equals(cl.tpc); // Ignore the status
    }
}
