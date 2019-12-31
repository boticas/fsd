package common;

import java.util.HashSet;

/**
 * common.Tweets
 */
public class Topics {
    private HashSet<String> topics;

    /**
     * Parameterized class builder.
     * 
     * @param topics
     */
    public Topics(HashSet<String> topics) {
        this.topics = topics;
    }

    /**
     * Get - tweets.
     * 
     * @return topics
     */
    public HashSet<String> getTopics() {
        return this.topics;
    }
}
