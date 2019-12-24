import java.util.ArrayList;

/**
 * GetTweets
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
        ArrayList<String> aux = new ArrayList<>(topics.size());
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
        if (this.topics == null)
            return new ArrayList<>();

        ArrayList<String> res = new ArrayList<>(this.topics.size());
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