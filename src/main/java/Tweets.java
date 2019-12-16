import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Tweets
 */
public class Tweets {
    /* nullKey will be set if the HashMap only contains one key (null) */
    private boolean nullKey;
    private HashMap<String, ArrayList<Tweet>> tweets;

    /**
     * Parameterized class builder.
     * 
     * @param tweets
     */
    public Tweets(ArrayList<Tweet> tweets) {
        this.nullKey = true;
        HashMap<String, ArrayList<Tweet>> aux = new HashMap<>();
        aux.put(null, tweets);
        this.tweets = aux;
    }

    /**
     * Parameterized class builder.
     * 
     * @param tweets
     */
    public Tweets(HashMap<String, ArrayList<Tweet>> tweets) {
        this.nullKey = false;
        this.tweets = tweets;
    }

    /**
     * Has only one entry - nullKey.
     * 
     * @param
     * @return true if the map only contains one key (null)
     */
    public boolean hasNullKey() {
        return this.nullKey;
    }

    /**
     * Get - tweets.
     * 
     * @param
     * @return tweets
     */
    public HashMap<String, ArrayList<Tweet>> getTweets() {
        HashMap<String, ArrayList<Tweet>> res = new HashMap<>(this.tweets.size());
        for (Map.Entry<String, ArrayList<Tweet>> e : this.tweets.entrySet()) {
            res.put(e.getKey(), e.getValue());
        }
        return res;
    }
}
