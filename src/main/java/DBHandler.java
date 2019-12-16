import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * DBHandler
 */
public class DBHandler {
    private int port;
    private ArrayList<Tweet> allTweets;
    private HashMap<String, ArrayList<Integer>> tweetsDB; // tópicos para tweets desse tópico
    private HashMap<String, ArrayList<String>> subscriptionsDB; // username para tópicos subscritos

    /**
     * Parameterized class builder.
     * 
     * @param port
     */
    public DBHandler(int port) {
        this.port = port;
        this.allTweets = new ArrayList<>();
        this.tweetsDB = new HashMap<>();
        this.subscriptionsDB = new HashMap<>();

        this.initializeDB();

        /* REMOVER */
        this.tweetsDB.forEach((k, v) -> {
            System.out.println("--- " + k + " ---");
            v.forEach((t) -> System.out.println(this.allTweets.get(t).getContent()));
        });
    }

    /**
     * Method that initializes the database.
     * 
     * @param
     * @return
     */
    private void initializeDB() {
        try {
            FileInputStream fi = new FileInputStream("db-" + this.port + ".data");
            ObjectInputStream oi = new ObjectInputStream(fi);

            this.allTweets = (ArrayList<Tweet>) oi.readObject();
            this.tweetsDB = (HashMap<String, ArrayList<Integer>>) oi.readObject();
            this.subscriptionsDB = (HashMap<String, ArrayList<String>>) oi.readObject();

            oi.close();
            fi.close();
        } catch (FileNotFoundException e) {
            System.out.println("No database found. Starting with an empty one");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that saves the database.
     * 
     * @param
     * @return
     */
    public void saveDB() {
        try {
            FileOutputStream f = new FileOutputStream("db-" + this.port + ".data");
            ObjectOutputStream o = new ObjectOutputStream(f);

            o.writeObject(this.allTweets);
            o.writeObject(this.tweetsDB);
            o.writeObject(this.subscriptionsDB);

            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that adds a tweet to the database and then saves it.
     * 
     * @param tweet
     * @return
     */
    public void addTweet(Tweet tweet) {
        int idx;
        synchronized (this.allTweets) {
            this.allTweets.add(tweet);
            idx = this.allTweets.size() - 1;
        }

        synchronized (this.tweetsDB) {
            ArrayList<String> topics = tweet.getTopics();
            for (String topic : topics) {
                ArrayList<Integer> tweets = this.tweetsDB.get(topic);
                if (tweets == null) {
                    tweets = new ArrayList<>();
                    this.tweetsDB.put(topic, tweets);
                }
                tweets.add(idx);
            }
            this.saveDB();
        }
    }

    /**
     * 
     * @param username
     * @param topics
     */
    public void updateSubscriptions(String username, ArrayList<String> topics) {
        synchronized (this.subscriptionsDB) {
            subscriptionsDB.put(username, topics);
            this.saveDB();
        }
    }

    /**
     * Get the last 10 tweets from the topics subscribed by the username.
     * 
     * @param username
     * @param topic
     * @return The last 10 tweets.
     */
    public ArrayList<Tweet> getLast10Tweets(String username, ArrayList<String> topics) {
        return new ArrayList<>();
    }

    /**
     * Get the last 10 tweets per topic subscribed by the username.
     * 
     * @param username
     * @param topic
     * @return The last 10 tweets per topic.
     */
    public ArrayList<Tweet> getLast10TweetsPerTopic(String username, ArrayList<String> topics) {
        ArrayList<Tweet> last10PerTopic = new ArrayList<>();

        ArrayList<String> subscriptions;
        synchronized (this.subscriptionsDB) {
            subscriptions = this.subscriptionsDB.get(username);
        }
        synchronized (this.tweetsDB) {
            for (String topic : topics) {
                if (subscriptions.contains(topic)) {
                    ArrayList<Integer> tweets = this.tweetsDB.get(topic);
                    int size = tweets.size();
                    for (int i = size - 1; i >= size - 10 && i >= 0; i--) {
                        last10PerTopic.add(allTweets.get(tweets.get(i)));
                    }
                }
            }
        }

        return last10PerTopic;
    }

    /**
     * Get all the topics subscribed by the username provided.
     * 
     * @param username
     * @return All the topics subscribed
     */
    public synchronized ArrayList<String> getTopics(String username) {
        return this.subscriptionsDB.get(username);
    }
}