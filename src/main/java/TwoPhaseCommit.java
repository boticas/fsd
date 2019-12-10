import java.util.ArrayList;

import io.atomix.utils.net.Address;

/**
 * TwoPhaseCommit
 */
public class TwoPhaseCommit {
    private int count;
    private boolean isHeartbeat;
    private Address coordinator;

    // Either tweet or username/topics are set; the others must be null
    private Tweet tweet;

    private String username;
    private ArrayList<String> topics;
    ///

    public TwoPhaseCommit(int count, Address coordinator) {
        this.count = count;
        this.isHeartbeat = true;
        this.coordinator = coordinator;
    }

    public TwoPhaseCommit(int count, Tweet tweet, Address coordinator) {
        this.count = count;
        this.tweet = tweet;
        this.coordinator = coordinator;
    }

    public TwoPhaseCommit(int count, String username, ArrayList<String> topics, Address coordinator) {
        this.count = count;
        this.username = username;
        ArrayList<String> aux = new ArrayList<String>(topics.size());
        for (String t : topics) {
            aux.add(t);
        }
        this.topics = aux;
        this.coordinator = coordinator;
    }

    /**
     * @return if it is an heartbeat
     */
    public boolean isHeartbeat() {
        return this.isHeartbeat;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the tweet
     */
    public Tweet getTweet() {
        return tweet;
    }

    /**
     * @return the topics
     */
    public ArrayList<String> getTopics() {
        ArrayList<String> res = new ArrayList<String>(this.topics.size());
        for (String t : topics) {
            res.add(t);
        }
        return res;
    }

    /**
     * @return the coordinator
     */
    public Address getCoordinator() {
        return coordinator;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        TwoPhaseCommit tpc = (TwoPhaseCommit) obj;
        if (this.count != tpc.count || this.isHeartbeat != tpc.isHeartbeat || !this.coordinator.equals(tpc.coordinator))
            return false;

        if (this.tweet != null)
            return this.tweet.equals(tpc.tweet);
        else
            return this.username.equals(tpc.username) && this.topics.equals(tpc.topics);
    }
}
