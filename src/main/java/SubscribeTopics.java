import java.util.ArrayList;

/**
 * SubscribeTopics
 */
public class SubscribeTopics {
    private ArrayList<String> topics;
    private String username;

    public SubscribeTopics(String username) {
        this.username = username;
    }

    public SubscribeTopics(ArrayList<String> topics, String username) {
        ArrayList<String> aux = new ArrayList<String>(topics.size());
        for (String t : topics) {
            aux.add(t);
        }
        this.topics = aux;
        this.username = username;
    }

    /**
     * @return the topics
     */
    public ArrayList<String> getTopics() {
        ArrayList<String> res = new ArrayList<String>(this.topics.size());
        for (String t : topics) {
            res.add(t);
        }
        return res;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }
}
