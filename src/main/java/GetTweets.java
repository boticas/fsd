import java.util.ArrayList;

/**
 * GetTweets
 */
public class GetTweets {
    // If the last 10 messages for any topic are wanted, leave topics == null
    // Otherwise, set it with the topics you want
    private ArrayList<String> topics;
    private String username;

    public GetTweets(String username) {
        this.username = username;
    }

    public GetTweets(ArrayList<String> topics, String username) {
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
