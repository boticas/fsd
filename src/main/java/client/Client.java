package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

import common.*;

/**
 * Client
 */
public class Client {
    private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private static String username;

    /**
     * Ler int
     * 
     * @return int
     */
    private static int readInt() {
        int res = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                res = Integer.parseInt(in.readLine());
            } catch (NumberFormatException exc) {
                System.out.println("Please, enter a number");
                continue;
            } catch (Exception exc) {
                exc.printStackTrace();
                break;
            }

            break;
        }

        return res;
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
        }
        for (Tweet tweet : tweets) {
            System.out.println(tweet.getUsername() + ": " + tweet.getContent());
        }
        waitConfirmation();
    }

    private static void getLast10PerTopic(ArrayList<Tweet> tweets) throws IOException {
        if (tweets.size() == 0) {
            System.out.println("There are no tweets for the requested topics");
        }
        for (Tweet tweet : tweets) {
            System.out.println(tweet.getUsername() + ": " + tweet.getContent());
        }
        waitConfirmation();
    }

    private static void viewTopics(Topics currentTopics) {
        if(currentTopics.getTopics().size() == 0) {
            System.out.println("You are not subscribed to any topics yet");
        } else {
            System.out.println("You are currently subscribed to:");
            for (String t : currentTopics.getTopics())
                System.out.println(t);
        }
    }

    private static SubscribeTopics subscribeTopics(Topics currentTopics) {
        if(currentTopics.getTopics().size() == 0) {
            System.out.println("You are not subscribed to any topics yet");
        } else {
            System.out.println("You are currently subscribed to:");
            for (String t : currentTopics.getTopics())
                System.out.println(t);
        }

        System.out.println("What new topics do you want to subscribe?");
        String input = "";
        try {
            input = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] topics = input.split(" ");
        HashSet<String> topicsList = new HashSet<String>(currentTopics.getTopics());
        for (String word : topics) {
            if (word.charAt(0) == '#')
                topicsList.add(word);
            else
                topicsList.add("#" + word);
        }
        SubscribeTopics res = new SubscribeTopics(topicsList, username);
        return res;
    }
    
    private static Tweet publishTweet() {
        System.out.println("Share something with the world!");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String tweet = "";
        try {
            tweet = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] words = tweet.split("[ ;\\.\\?!:,]+");
        HashSet<String> topics = new HashSet<>();
        for (String word : words) {
            if (word.charAt(0) == '#') 
                topics.add(word);
        }
        Tweet t = new Tweet(username, tweet, topics);
        return t;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Indique o endereço do cliente e o do servidor (ip:porta)");
            System.exit(1);
        }

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

        boolean exit = false;
        while (!exit) {
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

            byte[] res = null;
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
                    waitConfirmation();
                    break;
                case 2:
                    res = ms.sendAndReceive(server, "getTopics", serializer.encode(new GetTopics(username)), executor).get();
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
                    waitConfirmation();
                    break;
                case 3:
                    res = ms.sendAndReceive(server, "getTopics", serializer.encode(new GetTopics(username)), executor).get();
                    viewTopics(serializer.decode(res));
                    waitConfirmation();
                    break;
                case 4:
                    res = ms.sendAndReceive(server, "getTweets", serializer.encode(new GetTweets(username))).get();
                    getLast10(((Tweets) serializer.decode(res)).getTweets());
                    break;
                case 5:
                    System.out.println("What topics do you want to see?");
                    ArrayList<String> topicsList = new ArrayList<>();
                    String input = "";
                    try {
                        input = in.readLine();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String[] topicsToSee = input.split(" ");
                    for (String word : topicsToSee) {
                        if (word.charAt(0) == '#')
                            topicsList.add(word);
                        else
                            topicsList.add("#" + word);
                    }
                    res = ms.sendAndReceive(server, "getTweets", serializer.encode(new GetTweets(topicsList, username))).get();
                    getLast10(((Tweets) serializer.decode(res)).getTweets());
                    break;
                case 6:
                    clearView();
                    exit = true;
                    System.exit(1);
                    break;
            }
        }
    }
}
