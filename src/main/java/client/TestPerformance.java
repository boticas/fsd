package client;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import common.Tweet;
import common.TwitterSerializer;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

/**
 * TestPerformance
 */
public class TestPerformance {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Indique o endereço do cliente e o do servidor (ip:porta)");
            System.exit(1);
        }

        Address myAddress = Address.from(args[0]);
        Address server = Address.from(args[1]);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Serializer serializer = TwitterSerializer.serializer;

        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());
        AtomicInteger count = new AtomicInteger(0);
        ms.registerHandler("result", (a, b) -> {
            count.incrementAndGet();
        }, executor);
        ms.start().get();

        Thread counter = new Counter(count);
        counter.start();

        HashSet<String> topics = new HashSet<>();
        topics.add("testTopic");

        Tweet t = new Tweet("testUser", "testTweet", topics);
        while (true) {
            ms.sendAsync(server, "publishTweet", serializer.encode(t));
            try {
                Thread.sleep(20);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}

class Counter extends Thread {
    AtomicInteger count;

    public Counter(AtomicInteger count) {
        this.count = count;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            System.out.println(count.getAndSet(0));
        }
    }
}
