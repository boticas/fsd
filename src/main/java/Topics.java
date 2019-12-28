import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Tweets
 */
public class Topics {
    private HashSet<String> topics;

    /**
     * Parameterized class builder.
     * 
     * @param topics
     */
    public Topics(HashSet<String> topics) {
        HashSet<String> newTopics = new HashSet<>(topics.size());
        for (String t: topics) {
            newTopics.add(t);
        }
        this.topics = newTopics;
    }

    /**
     * Get - tweets.
     * 
     * @param
     * @return topics
     */
    public List<String> getTopics() {
        ArrayList<String> res = new ArrayList<>(this.topics.size());
        for (String t: this.topics) {
            res.add(t);
        }
        return res;
    }
}
