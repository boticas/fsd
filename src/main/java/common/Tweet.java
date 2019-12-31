package common;

import java.io.Serializable;
import java.util.HashSet;

/**
 * common.Tweet
 * 
 * Class that contains all the information about a single tweet posted by a user
 */
public class Tweet implements Serializable {
    private String username;
    private String content;
    private HashSet<String> topics;

    /**
     * Parameterized class builder.
     * 
     * @param username
     * @param content
     * @param topics
     */
    public Tweet(String username, String content, HashSet<String> topics) {
        this.username = username;
        this.content = content;
        this.topics = topics;
    }

    /**
     * Get - username.
     * 
     * @return username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Get - content.
     * 
     * @return content
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Get - topics.
     * 
     * @param
     * @return topics
     */
    public HashSet<String> getTopics() {
        return this.topics;
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

        Tweet t = (Tweet) obj;
        return this.username.equals(t.username) && this.content.equals(t.content) && this.topics.equals(t.topics);
    }
}