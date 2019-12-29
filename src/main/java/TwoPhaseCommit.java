import java.util.HashSet;

import io.atomix.utils.net.Address;

/**
 * TwoPhaseCommit
 */
public class TwoPhaseCommit {
    private int count;
    private boolean isHeartbeat;
    private Address coordinator;
    private Address requester;

    /* Either tweet, username/topics or serverJoin are set; the others must be null */
    private Tweet tweet;

    private String username;
    private HashSet<String> topics;

    private ServerJoin serverJoin;

    /**
     * Constructor for an heartbeat
     * 
     * @param count
     * @param coordinator
     */
    public TwoPhaseCommit(int count, Address coordinator) {
        this.count = count;
        this.isHeartbeat = true;
        this.coordinator = coordinator;
    }

    /**
     * Constructor for a tweet addition
     * 
     * @param count
     * @param tweet
     * @param coordinator
     */
    public TwoPhaseCommit(int count, Tweet tweet, Address coordinator, Address requester) {
        this.count = count;
        this.tweet = tweet;
        this.coordinator = coordinator;
        this.requester = requester;
    }

    /**
     * Constructor for subscriptions update
     * 
     * @param count
     * @param username
     * @param topics
     * @param coordinator
     */
    public TwoPhaseCommit(int count, String username, HashSet<String> topics, Address coordinator, Address requester) {
        this.count = count;
        this.username = username;
        HashSet<String> aux = new HashSet<String>(topics.size());
        topics.forEach((t) -> aux.add(t));
        this.topics = aux;
        this.coordinator = coordinator;
        this.requester = requester;
    }

    /**
     * Constructor for a server addition
     * 
     * @param count
     * @param tweet
     * @param coordinator
     */
    public TwoPhaseCommit(int count, ServerJoin serverJoin, Address coordinator, Address requester) {
        this.count = count;
        this.serverJoin = serverJoin;
        this.coordinator = coordinator;
        this.requester = requester;
    }

    /**
     * Get - isHeartbeat.
     * 
     * @param
     * @return isHeartbeat
     */
    public boolean isHeartbeat() {
        return this.isHeartbeat;
    }

    /**
     * Get - count.
     * 
     * @param
     * @return count
     */
    public int getCount() {
        return count;
    }

    /**
     * Get - username.
     * 
     * @param
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get - tweet.
     * 
     * @param
     * @return tweet
     */
    public Tweet getTweet() {
        return tweet;
    }

    /**
     * Get - topics.
     * 
     * @param
     * @return topics
     */
    public HashSet<String> getTopics() {
        HashSet<String> res = new HashSet<String>(this.topics.size());
        this.topics.forEach((t) -> res.add(t));
        return res;
    }

    /**
     * Get - coordinator.
     * 
     * @param
     * @return coordinator
     */
    public Address getCoordinator() {
        return coordinator;
    }

    /**
     * @return the requester
     */
    public Address getRequester() {
        return requester;
    }

    /**
     * @return the serverJoin
     */
    public ServerJoin getServerJoin() {
        return serverJoin;
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

        TwoPhaseCommit tpc = (TwoPhaseCommit) obj;
        if (this.count != tpc.count || this.isHeartbeat != tpc.isHeartbeat || !this.coordinator.equals(tpc.coordinator) || !this.requester.equals(tpc.requester))
            return false;

        if (this.tweet != null)
            return this.tweet.equals(tpc.tweet);
        else if (this.username != null && this.topics != null)
            return this.username.equals(tpc.username) && this.topics.equals(tpc.topics);
        else
            return this.serverJoin.equals(tpc.serverJoin);
    }
}