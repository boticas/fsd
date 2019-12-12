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
    private HashMap<String, ArrayList<Tweet>> tweetsDB;
    private HashMap<String, ArrayList<String>> subscriptionsDB;

    /**
     * Parameterized class builder.
     * 
     * @param port
     */
    public DBHandler(int port) {
        this.port = port;
        this.tweetsDB = new HashMap<>();
        this.subscriptionsDB = new HashMap<>();

        this.initializeDB();

        /* REMOVER */
        this.tweetsDB.forEach((k, v) -> {
            System.out.println("--- " + k + " ---");
            v.forEach((t) -> System.out.println(t.getContent()));
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

            this.tweetsDB = (HashMap<String, ArrayList<Tweet>>) oi.readObject();
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
        synchronized (this.tweetsDB) {
            ArrayList<String> topics = tweet.getTopics();
            for (String topic : topics) {
                ArrayList<Tweet> tweets = this.tweetsDB.get(topic);
                if (tweets == null) {
                    tweets = new ArrayList<>();
                    this.tweetsDB.put(topic, tweets);
                }
                tweets.add(tweet);
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
        // Update user's subscription to be equal to topics
    }
}
