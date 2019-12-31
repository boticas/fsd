package common;

import java.util.ArrayList;

/**
 * common.GetTweets
 */
public class GetTweets {
    /*
     * If the last 10 messages for any topic are wanted, leave topics == null.
     * Otherwise, set it with the topics you want.
     */
    private ArrayList<String> topics;
    private String username;

    /**
     * Parameterized class builder.
     * 
     * @param username
     */
    public GetTweets(String username) {
        this.username = username;
    }

    /**
     * Parameterized class builder.
     * 
     * @param topics
     * @param username
     */
    public GetTweets(ArrayList<String> topics, String username) {
        this.topics = topics;
        this.username = username;
    }

    /**
     * Get - topics.
     * 
     * @return topics
     */
    public ArrayList<String> getTopics() {
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