package common;

import java.util.ArrayList;

/**
 * common.Tweets
 */
public class Tweets {
    private ArrayList<Tweet> tweets;

    /**
     * Parameterized class builder.
     * 
     * @param tweets
     */
    public Tweets(ArrayList<Tweet> tweets) {
        this.tweets = tweets;
    }

    /**
     * Get - tweets.
     * 
     * @return tweets
     */
    public ArrayList<Tweet> getTweets() {
        return this.tweets;
    }
}
