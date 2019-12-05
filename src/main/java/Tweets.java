import java.util.HashMap;
import java.util.Map;

/**
 * Tweets
 */
public class Tweets {
    // nullKey will be set if the HashMap only contains one key (null)
    private boolean nullKey;
    private HashMap<String, Tweet> tweets;

    public Tweets(HashMap<String, Tweet> tweets) {
        this.nullKey = true;
        HashMap<String, Tweet> aux = new HashMap<String, Tweet>(tweets.size());
        for (Map.Entry<String, Tweet> e : tweets.entrySet()) {
            aux.put(e.getKey(), e.getValue());
        }
        this.tweets = aux;
    }

    /**
     * @return true if the map only contains one key (null)
     */
    public boolean getNullKey() {
        return this.nullKey;
    }

    /**
     * @return the tweets
     */
    public HashMap<String, Tweet> getTweets() {
        HashMap<String, Tweet> res = new HashMap<String, Tweet>(this.tweets.size());
        for (Map.Entry<String, Tweet> e : this.tweets.entrySet()) {
            res.put(e.getKey(), e.getValue());
        }
        return res;
    }
}
