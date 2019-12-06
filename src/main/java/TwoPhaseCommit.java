import java.util.ArrayList;

/**
 * TwoPhaseCommit
 */
public class TwoPhaseCommit {
    // Either tweet or username/topics are set; the others must be null
    private Tweet tweet;

    private String username;
    private ArrayList<String> topics;

    public TwoPhaseCommit(Tweet tweet) {
        this.tweet = tweet;
    }

    public TwoPhaseCommit(String username, ArrayList<String> topics) {
        this.username = username;
        ArrayList<String> aux = new ArrayList<String>(topics.size());
        for (String t : topics) {
            aux.add(t);
        }
        this.topics = aux;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        TwoPhaseCommit tpc = (TwoPhaseCommit) obj;
        if (this.tweet != null)
            return this.tweet.equals(tpc.tweet);
        else
            return this.username.equals(tpc.username) && this.topics.equals(tpc.topics);
    }
}
