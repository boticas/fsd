package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import common.Tweet;
import io.atomix.utils.net.Address;

/**
 * server.DBHandler
 */
public class DBHandler {
    private int port;
    private HashSet<Address> servers;
    private ArrayList<Tweet> allTweets;
    private HashMap<String, ArrayList<Integer>> tweetsDB; // tópicos para indices de tweets desse tópico
    private HashMap<String, HashSet<String>> subscriptionsDB; // username para tópicos subscritos

    /**
     * Parameterized class builder.
     * 
     * @param port
     */
    public DBHandler(Address address) {
        this.port = address.port();
        this.servers = new HashSet<>();
        this.servers.add(address);
        this.allTweets = new ArrayList<>();
        this.tweetsDB = new HashMap<>();
        this.subscriptionsDB = new HashMap<>();

        this.initializeDB();
    }

    /**
     * Parameterized class builder.
     * 
     * @param port
     */
    public DBHandler(int port, HashSet<Address> servers, ArrayList<Tweet> allTweets,
            HashMap<String, ArrayList<Integer>> tweetsDB, HashMap<String, HashSet<String>> subscriptionsDB) {
        this.port = port;
        this.servers = servers;
        this.allTweets = allTweets;
        this.tweetsDB = tweetsDB;
        this.subscriptionsDB = subscriptionsDB;

        this.saveDB();
    }

    /**
     * Method that checks if there is a database.
     * 
     * @param
     * @return
     */
    public static boolean checkDB(int port) {
        File aux = new File("db-" + port + ".data");
        return aux.exists();
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

            ArrayList<String> addresses = (ArrayList<String>) oi.readObject();
            addresses.forEach((a) -> this.servers.add(Address.from(a)));
            this.allTweets = (ArrayList<Tweet>) oi.readObject();
            this.tweetsDB = (HashMap<String, ArrayList<Integer>>) oi.readObject();
            this.subscriptionsDB = (HashMap<String, HashSet<String>>) oi.readObject();

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

            ArrayList<String> addresses = new ArrayList<>();
            this.servers.forEach((a) -> addresses.add(a.toString()));
            o.writeObject(addresses);
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
            HashSet<String> topics = tweet.getTopics();
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
    public void updateSubscriptions(String username, HashSet<String> topics) {
        synchronized (this.subscriptionsDB) {
            subscriptionsDB.put(username, topics);
            this.saveDB();
        }
    }

    /**
     * 
     * @param username
     * @param topics
     */
    public void addServer(Address address) {
        synchronized (this.servers) {
            this.servers.add(address);
            this.saveDB();
        }
    }

    /**
     * Get the last 10 tweets from the topics subscribed by the username.
     * 
     * @param username
     * @return The last 10 tweets.
     */
    public ArrayList<Tweet> getLast10Tweets(String username) {
        ArrayList<Tweet> last10 = new ArrayList<>();

        HashSet<String> subscriptions;
        synchronized (this.subscriptionsDB) {
            subscriptions = this.subscriptionsDB.get(username);
        }

        synchronized (this.tweetsDB) {
            synchronized (this.allTweets) {
                HashSet<Integer> tweets = new HashSet<>();
                for (String topic : subscriptions) {
                    tweets.addAll(tweetsDB.get(topic));
                }
                int i = 0;
                for (int latest = allTweets.size() - 1; latest >= 0 && i < 10; latest--) {
                    if (tweets.contains(latest)) {
                        last10.add(allTweets.get(latest));
                        i++;
                    }
                }
            }
        }
        return last10;
    }

    /**
     * Get the last 10 tweets per topic subscribed by the username.
     * 
     * @param username
     * @param topics
     * @return The last 10 tweets per topic.
     */
    public ArrayList<Tweet> getLast10TweetsPerTopic(String username, ArrayList<String> topics) {
        ArrayList<Tweet> last10 = new ArrayList<>();

        synchronized (this.tweetsDB) {
            synchronized (this.allTweets) {
                HashSet<Integer> tweets = new HashSet<>();
                for (String topic : topics) {
                    ArrayList<Integer> tweetsForTopic = tweetsDB.get(topic);
                    if(tweetsForTopic != null)
                        tweets.addAll(tweetsForTopic);
                }
                int i = 0;
                for (int latest = allTweets.size() - 1; latest >= 0 && i < 10; latest--) {
                    if (tweets.contains(latest)) {
                        last10.add(allTweets.get(latest));
                        i++;
                    }
                }
            }
        }
        return last10;
    }

    /**
     * Get all the topics subscribed by the username provided.
     * 
     * @param username
     * @return All the topics subscribed
     */
    public HashSet<String> getTopics(String username) {
        synchronized (this.subscriptionsDB) {
            HashSet<String> subscriptions = this.subscriptionsDB.get(username);
            if (subscriptions == null)
                return new HashSet<>();

            return subscriptions;
        }
    }

    /**
     * @return the allTweets
     */
    public ArrayList<Tweet> getAllTweets() {
        return allTweets;
    }

    /**
     * @return the tweetsDB
     */
    public HashMap<String, ArrayList<Integer>> getTweetsDB() {
        return tweetsDB;
    }

    /**
     * @return the subscriptionsDB
     */
    public HashMap<String, HashSet<String>> getSubscriptionsDB() {
        return subscriptionsDB;
    }

    /**
     * @return the servers
     */
    public HashSet<Address> getServers() {
        return servers;
    }
}
