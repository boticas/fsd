package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import common.Tweet;
import io.atomix.utils.net.Address;

/**
 * server.ServerJoinResponse
 */
public class ServerJoinResponse {
    private HashSet<Address> servers;

    private ArrayList<Tweet> allTweets;
    private HashMap<String, ArrayList<Integer>> tweetsDB; // Map of topics to indices of tweets on that topics
    private HashMap<String, HashSet<String>> subscriptionsDB; // Map of usernames to subscribed topics

    public ServerJoinResponse() {

    }

    public ServerJoinResponse(HashSet<Address> servers, ArrayList<Tweet> allTweets,
            HashMap<String, ArrayList<Integer>> tweetsDB, HashMap<String, HashSet<String>> subscriptionsDB) {
        this.servers = servers;
        this.allTweets = allTweets;
        this.tweetsDB = tweetsDB;
        this.subscriptionsDB = subscriptionsDB;
    }

    /**
     * @return the servers
     */
    public HashSet<Address> getServers() {
        return servers;
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
     * @param servers the servers to set
     */
    public void set(ServerJoinResponse other) {
        this.servers = other.servers;
        this.allTweets = other.allTweets;
        this.tweetsDB = other.tweetsDB;
        this.subscriptionsDB = other.subscriptionsDB;
    }
}
