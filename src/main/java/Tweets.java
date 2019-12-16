import java.util.ArrayList;
import java.util.List;

/**
 * Tweets
 */
public class Tweets {
    private ArrayList<Tweet> tweets;

    /**
     * Parameterized class builder.
     * 
     * @param tweets
     */
    public Tweets(ArrayList<Tweet> tweets) {
        ArrayList<Tweet> newTweets = new ArrayList<>(tweets.size());
        for (Tweet t: tweets) {
            newTweets.add(t);
        }
        this.tweets = newTweets;
    }

    /**
     * Get - tweets.
     * 
     * @param
     * @return tweets
     */
    public List<Tweet> getTweets() {
        ArrayList<Tweet> res = new ArrayList<>(this.tweets.size());
        for (Tweet t: this.tweets) {
            res.add(t);
        }
        return res;
    }
}
