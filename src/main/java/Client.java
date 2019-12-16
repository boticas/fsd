import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static void getTop10() {

    }

    private static SubscribeTopics subscribeTopics() {
        System.out.println("What topics do you want to subscribe?");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        try {
            input = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] topics = input.split(" ");
        ArrayList<String> topicsList = new ArrayList<String>();
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

    public static void main(String[] args) {
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
        ms.start();
        ms.registerHandler("result", (a, b) -> {
            Response res = serializer.decode(b);
            if (res.getSuccess())
                System.out.println("Thanks for sharing!");
            else
                System.out.println("Sorry, try again later.");
        }, executor);
        while (!ms.isRunning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        boolean exit = false;
        
        while(!exit) {
            StringBuilder main = new StringBuilder();
            main.append("What do you want to do? (Select number)\n");
            main.append("1 - Tweet\n");
            main.append("2 - Subscribe\n");
            main.append("3 - Top 10\n");
            main.append("4 - Exit\n");
            
            clearView();
            System.out.println(main);
            
            int escolha;
            do{
                escolha = readInt();
            }while(escolha < 1 || escolha > 4);
            
            switch(escolha) {
                case 1:
                    Tweet t = publishTweet();
                    ms.sendAsync(server, "publishTweet", serializer.encode(t));
                    break;
                case 2:
                    SubscribeTopics topics = subscribeTopics();
                    ms.sendAsync(server, "subscribeTopics", serializer.encode(topics));
                    break;
                case 3:
                    getTop10();
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
