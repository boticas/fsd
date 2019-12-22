/**
 * Log
 */
public class Log {
    public enum Status {
        PREPARE, COMMIT, ABORT
    }

    private TwoPhaseCommit tpc;
    private Status status;

    /**
     * Get - tpc.
     * 
     * @param
     * @return tpc
     */
    public TwoPhaseCommit getTpc() {
        return tpc;
    }

    /**
     * Get - status.
     * 
     * @param
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Set - tpc.
     * 
     * @param tpc
     * @return
     */
    public void setTpc(TwoPhaseCommit tpc) {
        this.tpc = tpc;
    }

    /**
     * Set - status.
     * 
     * @param status
     * @return
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Method hashCode.
     * 
     * @param
     * @return int
     */
    @Override
    public int hashCode() {
        if (this.tpc.getTweet() != null)
            return (int) this.tpc.getTweet().getContent().charAt(0);
        else
            return (int) this.tpc.getUsername().charAt(0);
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