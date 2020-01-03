package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import common.GetTopics;
import common.GetTweets;
import common.Response;
import common.SubscribeTopics;
import common.Topics;
import common.Tweet;
import common.Tweets;
import common.TwitterSerializer;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

/**
 * client.Client
 */
public class Client {
    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private static String username;

    /**
     * Ler int
     * 
     * @return int
     */
    private static int readInt() throws IOException {
        while (true) {
            try {
                return Integer.parseInt(in.readLine());
            } catch (NumberFormatException exc) {
                System.out.println("Please, enter a number");
            }
        }
    }

    private static void waitConfirmation() throws IOException {
        System.out.print("\nPress ENTER to continue...");
        System.out.flush();
        in.readLine();
    }

    private static void clearView() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void getLast10(ArrayList<Tweet> tweets) throws IOException {
        if (tweets.size() == 0) {
            System.out.println("There are no tweets for your subscriptions or you don't have any subscriptions");
        } else {
            for (Tweet tweet : tweets)
                System.out.println(tweet.getUsername() + ": " + tweet.getContent());
        }
    }

    private static void getLast10PerTopic(ArrayList<Tweet> tweets) throws IOException {
        if (tweets.size() == 0) {
            System.out.println("There are no tweets for the requested topics");
        } else {
            for (Tweet tweet : tweets)
                System.out.println(tweet.getUsername() + ": " + tweet.getContent());
        }
    }

    private static void viewTopics(Topics currentTopics) {
        if (currentTopics.getTopics().size() == 0) {
            System.out.println("You are not subscribed to any topics yet");
        } else {
            System.out.println("You are currently subscribed to:");
            for (String t : currentTopics.getTopics())
                System.out.println(t);
        }
    }

    private static SubscribeTopics subscribeTopics(Topics currentTopics) throws IOException {
        if (currentTopics.getTopics().size() == 0) {
            System.out.println("You are not subscribed to any topics yet");
        } else {
            System.out.println("You are currently subscribed to:");
            for (String t : currentTopics.getTopics())
                System.out.println(t);
        }

        System.out.println("What new topics do you want to subscribe?");
        String input = in.readLine();
        String[] topics = input.split(" ");
        HashSet<String> topicsList = new HashSet<String>(currentTopics.getTopics());
        for (String word : topics) {
            if (word.length() == 0)
                continue;

            if (word.charAt(0) == '#')
                topicsList.add(word);
            else
                topicsList.add("#" + word);
        }

        return new SubscribeTopics(topicsList, username);
    }

    private static Tweet publishTweet() throws IOException {
        System.out.println("Share something with the world!");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String tweet = in.readLine();
        String[] words = tweet.split("[ ;\\.\\?!:,]+");
        HashSet<String> topics = new HashSet<>();
        for (String word : words) {
            if (word.length() == 0)
                continue;

            if (word.charAt(0) == '#')
                topics.add(word);
        }

        return new Tweet(username, tweet, topics);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Indique o endereço do cliente e o do servidor (ip:porta)");
            System.exit(1);
        }

        clearView();
        System.out.println("Welcome to Twitter");
        System.out.print("Please introduce your username: ");
        System.out.flush();
        username = in.readLine();

        Address myAddress = Address.from(args[0]);
        Address server = Address.from(args[1]);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Serializer serializer = TwitterSerializer.serializer;

        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());
        Response ok = new Response(false);
        AtomicBoolean ab = new AtomicBoolean(false);
        ms.registerHandler("result", (a, b) -> {
            synchronized (ab) {
                ab.set(true);
                Response x = serializer.decode(b);
                ok.setSuccess(x.getSuccess());
                ab.notify();
            }
        }, executor);
        ms.start().get();

        while (true) {
            StringBuilder main = new StringBuilder();
            main.append("What do you want to do? (Select number)\n");
            main.append("1 - Tweet\n");
            main.append("2 - Subscribe\n");
            main.append("3 - View subscriptions\n");
            main.append("4 - Last 10 from all subscribed topics\n");
            main.append("5 - Last 10 from specific topics\n");
            main.append("6 - Exit\n");

            clearView();
            System.out.println(main);

            int escolha;
            do {
                escolha = readInt();
            } while (escolha < 1 || escolha > 6);

            byte[] res;
            switch (escolha) {
                case 1:
                    Tweet t = publishTweet();
                    ms.sendAsync(server, "publishTweet", serializer.encode(t));
                    synchronized (ab) {
                        while (!ab.get())
                            ab.wait();
                        ab.set(false);
                    }
                    if (ok.getSuccess())
                        System.out.println("Thanks for sharing!");
                    else
                        System.out.println("Sorry, try again later.");
                    break;
                case 2:
                    res = ms.sendAndReceive(server, "getTopics", serializer.encode(new GetTopics(username)), executor)
                            .get();
                    SubscribeTopics topics = subscribeTopics(serializer.decode(res));
                    ms.sendAsync(server, "subscribeTopics", serializer.encode(topics));
                    synchronized (ab) {
                        while (!ab.get())
                            ab.wait();
                        ab.set(false);
                    }
                    if (ok.getSuccess())
                        System.out.println("Subscriptions updated!");
                    else
                        System.out.println("Sorry, try again later.");
                    break;
                case 3:
                    res = ms.sendAndReceive(server, "getTopics", serializer.encode(new GetTopics(username)), executor)
                            .get();
                    viewTopics(serializer.decode(res));
                    break;
                case 4:
                    res = ms.sendAndReceive(server, "getTweets", serializer.encode(new GetTweets(username))).get();
                    getLast10(((Tweets) serializer.decode(res)).getTweets());
                    break;
                case 5:
                    System.out.println("What topics do you want to see?");
                    ArrayList<String> topicsList = new ArrayList<>();
                    String input = in.readLine();
                    String[] topicsToSee = input.split(" ");
                    for (String word : topicsToSee) {
                        if (word.length() == 0)
                            continue;

                        if (word.charAt(0) == '#')
                            topicsList.add(word);
                        else
                            topicsList.add("#" + word);
                    }
                    res = ms.sendAndReceive(server, "getTweets", serializer.encode(new GetTweets(topicsList, username)))
                            .get();
                    getLast10PerTopic(((Tweets) serializer.decode(res)).getTweets());
                    break;
                case 6:
                    clearView();
                    System.exit(1);
                    break;
            }
            waitConfirmation();
        }
    }
}
