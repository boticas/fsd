import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Scanner;

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

    /**
     * Ler int
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
        try {
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    private static void clearView() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void getTop10() {
        
        
    }
    
    private static void subscribeTopic() {
        
    }
    
    private static Tweet publishTweet() {
        System.out.println("Share something with the world!");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String tweet = "";
        try {
            tweet = in.readLine();
        } catch(Exception e){
            e.printStackTrace();
        }
        String[] words = tweet.split(" ");
        ArrayList<String> topics = new ArrayList<>();
        for (String word : words) {
            // Problema: Cortar pontuações nos extremos
            if(word.charAt(0) == '#')
                topics.add(word);
        }
        Tweet t = new Tweet("User", tweet, topics);
        return t;
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Indique o endereço do cliente e os dos servidores (ip:porta)");
            System.exit(1);
        }
        
        Address myAddress = Address.from(args[0]);
        ArrayList<Address> servers = new ArrayList<Address>(args.length - 1);
        for (int i = 1; i < args.length; i++)
        servers.add(Address.from(args[i]));
        
        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());
        ms.start();
        ms.registerHandler("Response", arg1, arg2);
        
        Serializer tweetSerializer = new SerializerBuilder().addType(Tweet.class).build();
        
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
                    ms.sendAsync(servers.get(myAddress.port() % servers.size()), "publishTweet", tweetSerializer.encode(t));
                    
                    break;
                case 2:
                    subscribeTopic();
                    break;
                case 3:
                    getTop10();
                    break;
                case 4:
                    clearView();
                    exit = true;
                    break;
            }
        }

        for (int i = 0; i < 25; i++) {
            ArrayList<String> topics = new ArrayList<>();
            topics.add("t" + myAddress.port());
            Tweet t = new Tweet("User", String.valueOf(i), topics);
            ms.sendAsync(servers.get(myAddress.port() % servers.size()), "publishTweet", tweetSerializer.encode(t));
        }
    }
}
