import java.util.HashSet;

/**
 * SubscribeTopics
 */
public class SubscribeTopics {
    private HashSet<String> topics;
    private String username;

    /**
     * Parameterized class builder
     * 
     * @param username
     */
    public SubscribeTopics(String username) {
        this.username = username;
    }

    /**
     * Parameterized class builder.
     * 
     * @param topics
     * @param username
     */
    public SubscribeTopics(HashSet<String> topics, String username) {
        HashSet<String> aux = new HashSet<String>(topics.size());
        topics.forEach((t) -> aux.add(t));
        this.topics = aux;
        this.username = username;
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
     * Get - username.
     * 
     * @param
     * @return username
     */
    public String getUsername() {
        return username;
    }
}