import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.*;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

/**
 * Client
 */
public class Client {

    private static String username = "User";

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

    private static void clearView() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void getLast10(ArrayList<Tweet> tweets) {
        // TODO
    }

    private static SubscribeTopics subscribeTopics(Topics currentTopics) {
        System.out.println("You are currently subscribed to:");
        for (String t : currentTopics.getTopics()) {
            System.out.println(t);
        }
        System.out.println("What new topics do you want to subscribe?");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        try {
            input = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] topics = input.split(" ");
        ArrayList<String> topicsList = new ArrayList<String>(currentTopics.getTopics());
        for (String word : topics) {
            // Problema: Cortar pontuações nos extremos
            if (word.charAt(0) == '#')
                topicsList.add(word);
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
        String[] words = tweet.split(" ");
        ArrayList<String> topics = new ArrayList<>();
        for (String word : words) {
            // Problema: Cortar pontuações nos extremos
            if (word.charAt(0) == '#')
                topics.add(word);
        }
        Tweet t = new Tweet(username, tweet, topics);
        return t;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Indique o endereço do cliente e os do servidor (ip:porta)");
            System.exit(1);
        }

        Address myAddress = Address.from(args[0]);
        Address server = Address.from(args[1]);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Serializer serializer = new SerializerBuilder().addType(Tweet.class).addType(Topics.class).addType(Tweets.class)
                .addType(SubscribeTopics.class).addType(GetTweets.class).addType(GetTopics.class)
                .addType(Response.class).addType(TwoPhaseCommit.class).addType(Address.class).build();

        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());
        ms.registerHandler("result", (a, b) -> {
            Response res = serializer.decode(b);
            if (res.getSuccess())
                System.out.println("Thanks for sharing!");
            else
                System.out.println("Sorry, try again later.");
        }, executor);
        ms.start().get();

        boolean exit = false;
        while (!exit) {
            StringBuilder main = new StringBuilder();
            main.append("What do you want to do? (Select number)\n");
            main.append("1 - Tweet\n");
            main.append("2 - Subscribe\n");
            main.append("3 - Last 10\n");
            main.append("4 - Exit\n");

            clearView();
            System.out.println(main);

            int escolha;
            do {
                escolha = readInt();
            } while (escolha < 1 || escolha > 4);

            byte[] res = null;
            switch (escolha) {
                case 1:
                    Tweet t = publishTweet();
                    res = ms.sendAndReceive(server, "publishTweet", serializer.encode(t)).get();
                    Response response = serializer.decode(res);
                    if (response.getSuccess())
                        System.out.println("Thanks for sharing!");
                    else
                        System.out.println("Sorry, try again later.");
                    break;
                case 2:
                    try {
                        // RESOLVER: Espera por uma resposta indefinidamente
                        res = ms.sendAndReceive(server, "getTopics", serializer.encode(new GetTopics(username)), true, executor).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    SubscribeTopics topics = subscribeTopics(serializer.decode(res));
                    ms.sendAsync(server, "subscribeTopics", serializer.encode(topics));
                    break;
                case 3:
                    try {
                        res = ms.sendAndReceive(server, "getTweets", serializer.encode(new GetTweets(username))).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    getLast10(((Tweets)serializer.decode(res)).getTweets());
                    break;
                case 4:
                    clearView();
                    exit = true;
                    System.exit(1);
                    break;
            }
        }
    }
}
