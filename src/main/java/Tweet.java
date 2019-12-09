import java.io.Serializable;
import java.util.ArrayList;

/**
 * Tweet
 * 
 * Class that contains all the information about a single tweet posted by a user
 */
public class Tweet implements Serializable {
    private String username;
    private String content;
    private ArrayList<String> topics;

    public Tweet(String username, String content, ArrayList<String> topics) {
        this.username = username;
        this.content = content;
        ArrayList<String> aux = new ArrayList<String>(topics.size());
        for (String t : topics) {
            aux.add(t);
        }
        this.topics = aux;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
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
     * @return if the current tweet belongs to a certain topic
     */
    public boolean hasTopic(String topic) {
        return this.topics.contains(topic);
    }

    /**
     * order the topics in ascending order
     */
    public void orderTopics() {
        this.topics.sort((s1, s2) -> s1.compareTo(s2));
    }

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
