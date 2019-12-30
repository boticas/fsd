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
        HashSet<String> aux = new HashSet<String>(topics.size());
        topics.forEach((t) -> aux.add(t));
        this.topics = aux;
    }

    /**
     * Get - username.
     * 
     * @param
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get - content.
     * 
     * @param
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * Get - topics.
     * 
     * @param
     * @return topics
     */
    public HashSet<String> getTopics() {
        HashSet<String> res = new HashSet<String>(this.topics.size());
        this.topics.forEach((t) -> res.add(t));
        return res;
    }

    /**
     * Method that checks if the current tweet belongs to a certain topic.
     * 
     * @param topic
     * @return boolean
     */
    public boolean hasTopic(String topic) {
        return this.topics.contains(topic);
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