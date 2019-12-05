import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

public class Twitter {
    public static Pair<HashMap<String, ArrayList<Tweet>>, HashMap<String, ArrayList<String>>> initializeDB(int port) {
        HashMap<String, ArrayList<Tweet>> tweetsDB = new HashMap<>();
        HashMap<String, ArrayList<String>> subscriptionsDB = new HashMap<>();
        try {
            FileInputStream fi = new FileInputStream("db-" + port + ".data");
            ObjectInputStream oi = new ObjectInputStream(fi);

            tweetsDB = (HashMap<String, ArrayList<Tweet>>) oi.readObject();
            subscriptionsDB = (HashMap<String, ArrayList<String>>) oi.readObject();

            oi.close();
            fi.close();
        } catch (FileNotFoundException e) {
            System.out.println("No database found. Starting with an empty one");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Pair<HashMap<String, ArrayList<Tweet>>, HashMap<String, ArrayList<String>>>(tweetsDB,
                subscriptionsDB);
    }

    public static void saveDB(int port, HashMap<String, ArrayList<Tweet>> tweetsDB,
            HashMap<String, ArrayList<String>> subscriptionsDB) {
        try {
            FileOutputStream f = new FileOutputStream("db-" + port + ".data");
            ObjectOutputStream o = new ObjectOutputStream(f);

            o.writeObject(tweetsDB);
            o.writeObject(subscriptionsDB);

            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Indique as portas dos servidores (a do atual primeiro)");
            System.exit(1);
        }

        // Extract the ports of the servers
        int myPort = Integer.parseInt(args[0]);
        ArrayList<Integer> otherPorts = new ArrayList<Integer>(args.length - 1);
        for (int i = 1; i < args.length; i++)
            otherPorts.add(Integer.parseInt(args[i]));

        // Retreive the messages and the subscriptions from the storage if they exist
        Pair<HashMap<String, ArrayList<Tweet>>, HashMap<String, ArrayList<String>>> res = initializeDB(myPort);
        HashMap<String, ArrayList<Tweet>> tweetsDB = res.getFirst();
        ReentrantLock tweetsDBLock = new ReentrantLock();

        HashMap<String, ArrayList<String>> subscriptionsDB = res.getSecond();
        ReentrantLock subscriptionsDBLock = new ReentrantLock();

        // Get the server ready for receiving messages from its peers
        ExecutorService executor = Executors.newFixedThreadPool(1);

        ManagedMessagingService ms = new NettyMessagingService("twitter", Address.from(myPort), new MessagingConfig());
        ms.start();

        // Serializers for the messages received fom the clients
        Serializer publishTweetSerializer = new SerializerBuilder().addType(Tweet.class).build();
        Serializer subscribeTopicsSerializer = new SerializerBuilder().addType(SubscribeTopics.class).build();
        Serializer getTweetsSerializer = new SerializerBuilder().addType(GetTweets.class).build();
        Serializer getTopicsSerializer = new SerializerBuilder().addType(GetTopics.class).build();

        // Serializers for the messages sent to the clients
        Serializer responseSerializer = new SerializerBuilder().addType(Response.class).build();
        Serializer tweetsSerializer = new SerializerBuilder().addType(Tweets.class).build();

        // Serializers for the messages sent between the servers
        Serializer twoPhaseCommitSerializer = new SerializerBuilder().addType(TwoPhaseCommit.class).addType(Tweet.class)
                .build();

        ms.registerHandler("publishTweet", (a, b) -> {
            System.out.println("publishTweet");
            Tweet newTweet = publishTweetSerializer.decode(b);
            newTweet.orderTopics();

            TwoPhaseCommit prepare = new TwoPhaseCommit(newTweet);
            for (int port : otherPorts) {
                ms.sendAsync(Address.from("localhost", port), "tpcPrepare", twoPhaseCommitSerializer.encode(prepare));
            }
        }, executor);

        ms.registerHandler("subscribeTopics", (a, b) -> {
            System.out.println("subscribeTopics");
            SubscribeTopics st = subscribeTopicsSerializer.decode(b);

            TwoPhaseCommit prepare = new TwoPhaseCommit(st.getUsername(), st.getTopics());
            for (int port : otherPorts) {
                ms.sendAsync(Address.from("localhost", port), "tpcPrepare", twoPhaseCommitSerializer.encode(prepare));
            }
        }, executor);

        ms.registerHandler("getTweets", (a, b) -> {
            System.out.println("getTweets");
            GetTweets gt = getTweetsSerializer.decode(b);
            String username = gt.getUsername();
            ArrayList<String> topics = gt.getTopics();
            if (topics == null) {
                // Return last 10 tweets from the user's subscriptions
            } else {
                // Return last 10 tweets for each of the topics that the user is subscribed
            }
        }, executor);

        ms.registerHandler("getTopics", (a, b) -> {
            System.out.println("getTopics");
            GetTopics gt = getTopicsSerializer.decode(b);
            String username = gt.getUsername();
            // Return the topics username is subscribed to
        }, executor);

        ms.registerHandler("tpcPrepare", (a, b) -> {
            System.out.println("tpcPrepare");
            TwoPhaseCommit prepare = twoPhaseCommitSerializer.decode(b);

            prepare.setStatus(true);

            ms.sendAsync(Address.from("localhost", a.port()), "tpcResponse", twoPhaseCommitSerializer.encode(prepare));
        }, executor);

        ms.registerHandler("tpcResponse", (a, b) -> {
            System.out.println("tpcResponse");
            TwoPhaseCommit response = twoPhaseCommitSerializer.decode(b);

            if (response.getStatus()) {
                for (int port : otherPorts) {
                    ms.sendAsync(Address.from("localhost", port), "tpcCommit",
                            twoPhaseCommitSerializer.encode(response));
                }
            } else {
                for (int port : otherPorts) {
                    ms.sendAsync(Address.from("localhost", port), "tpcRollback",
                            twoPhaseCommitSerializer.encode(response));
                }
            }
        }, executor);

        ms.registerHandler("tpcCommit", (a, b) -> {
            System.out.println("tpcCommit");
            TwoPhaseCommit commit = twoPhaseCommitSerializer.decode(b);

            if (commit.getTweet() != null) {
                // Add the new tweet to the list of tweets for each of its topics
                Tweet newTweet = commit.getTweet();

                tweetsDBLock.lock();
                try {
                    ArrayList<String> topics = newTweet.getTopics();
                    for (String topic : topics) {
                        ArrayList<Tweet> tweets = tweetsDB.get(topic);
                        if (tweets == null) {
                            tweets = new ArrayList<>();
                            tweetsDB.put(topic, tweets);
                        }
                        tweets.add(newTweet);
                    }
                    saveDB(myPort, tweetsDB, subscriptionsDB);
                } catch (Exception e) {
                    e.printStackTrace();
                    tweetsDBLock.unlock();
                }
            } else {
                // Update username's subscriptions
            }
        }, executor);

        ms.registerHandler("tpcRollback", (a, b) -> {
            System.out.println("tpcRollback");
            TwoPhaseCommit rollback = twoPhaseCommitSerializer.decode(b);
        }, executor);
    }
}
