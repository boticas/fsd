import java.util.HashMap;
import java.util.Map;

/**
 * Tweets
 */
public class Tweets {
    /* nullKey will be set if the HashMap only contains one key (null) */
    private boolean nullKey;
    private HashMap<String, Tweet> tweets;

    /**
     * Parameterized class builder.
     * 
     * @param tweets
     */
    public Tweets(HashMap<String, Tweet> tweets) {
        this.nullKey = true;
        HashMap<String, Tweet> aux = new HashMap<String, Tweet>(tweets.size());
        for (Map.Entry<String, Tweet> e : tweets.entrySet()) {
            aux.put(e.getKey(), e.getValue());
        }
        this.tweets = aux;
    }

    /**
     * Get - nullKey.
     * 
     * @param
     * @return true if the map only contains one key (null)
     */
    public boolean getNullKey() {
        return this.nullKey;
    }

    /**
     * Get - tweets.
     * 
     * @param
     * @return tweets
     */
    public HashMap<String, Tweet> getTweets() {
        HashMap<String, Tweet> res = new HashMap<>(this.tweets.size());
        for (Map.Entry<String, Tweet> e : this.tweets.entrySet()) {
            res.put(e.getKey(), e.getValue());
        }
        return res;
    }
}