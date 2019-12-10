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

    public DBHandler(int port) {
        this.port = port;
        this.tweetsDB = new HashMap<>();
        this.subscriptionsDB = new HashMap<>();

        this.initializeDB();

        try {
            this.tweetsDB.forEach((k, v) -> {
                System.out.println("--- " + k + " ---");
                v.forEach((t) -> System.out.println(t.getContent()));
            });
        } catch (Exception e) {
        }
    }

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

    public void updateSubscriptions(String username, ArrayList<String> topics) {
        // Update user's subscription to be equal to topics
    }
}
