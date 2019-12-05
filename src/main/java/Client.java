import java.util.ArrayList;
import java.util.Random;

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
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Indique a porta do cliente e a dos servidores");
            System.exit(1);
        }

        int myPort = Integer.parseInt(args[0]);
        ArrayList<Integer> servers = new ArrayList<Integer>(args.length - 1);
        for (int i = 1; i < args.length; i++)
            servers.add(Integer.parseInt(args[i]));

        ManagedMessagingService ms = new NettyMessagingService("twitter", Address.from(myPort), new MessagingConfig());
        ms.start();

        Serializer tweetSerializer = new SerializerBuilder().addType(Tweet.class).build();

        ArrayList<String> topics = new ArrayList<>();
        topics.add("#first");
        topics.add("#tweet");
        Tweet t = new Tweet("User", "My first tweet", topics);

        for (int i = 0; i < 10; i++) {
            ms.sendAsync(Address.from("localhost", servers.get(new Random().nextInt(servers.size()))), "publishTweet",
                    tweetSerializer.encode(t));
        }
    }
}
