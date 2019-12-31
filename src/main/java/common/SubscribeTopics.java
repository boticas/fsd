package common;

import java.util.HashSet;

/**
 * common.SubscribeTopics
 */
public class SubscribeTopics {
    private HashSet<String> topics;
    private String username;

    /**
     * Parameterized class builder.
     * 
     * @param topics
     * @param username
     */
    public SubscribeTopics(HashSet<String> topics, String username) {
        this.topics = topics;
        this.username = username;
    }

    /**
     * Get - topics.
     * 
     * @return topics
     */
    public HashSet<String> getTopics() {
        return this.topics;
    }

    /**
     * Get - username.
     * 
     * @return username
     */
    public String getUsername() {
        return this.username;
    }
}