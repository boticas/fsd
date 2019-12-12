import java.util.ArrayList;

/**
 * SubscribeTopics
 */
public class SubscribeTopics {
    private ArrayList<String> topics;
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
    public SubscribeTopics(ArrayList<String> topics, String username) {
        ArrayList<String> aux = new ArrayList<String>(topics.size());
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
    public ArrayList<String> getTopics() {
        ArrayList<String> res = new ArrayList<String>(this.topics.size());
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