import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

public class Twitter {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Indique o endereço deste servidor e os dos outros servidores (ip:porta)");
            System.exit(1);
        }

        // Extract the ports of the servers
        Address myAddress = Address.from(args[0]);
        ArrayList<Address> otherAddresses = new ArrayList<Address>(args.length - 1);
        for (int i = 1; i < args.length; i++)
            otherAddresses.add(Address.from(args[i]));

        // Setup the infrastructure related to DB managment
        DBHandler dbHandler = new DBHandler(myAddress.port());

        // Setup the infrastructure related to TPC and respective logs
        TPCLogger tpcLogger = new TPCLogger(otherAddresses.size() + 1, myAddress.port());

        // Get the server ready for receiving messages from its peers
        ExecutorService executor = Executors.newFixedThreadPool(1);

        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());
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

            tpcLogger.updateCoordinatorLog(prepare, CoordinatorLog.Status.STARTED);

            ms.sendAsync(myAddress, "tpcPrepare", twoPhaseCommitSerializer.encode(prepare));
            for (Address address : otherAddresses) {
                ms.sendAsync(address, "tpcPrepare", twoPhaseCommitSerializer.encode(prepare));
            }
        }, executor);

        ms.registerHandler("subscribeTopics", (a, b) -> {
            System.out.println("subscribeTopics");
            SubscribeTopics st = subscribeTopicsSerializer.decode(b);

            TwoPhaseCommit prepare = new TwoPhaseCommit(st.getUsername(), st.getTopics());

            tpcLogger.updateCoordinatorLog(prepare, CoordinatorLog.Status.STARTED);

            ms.sendAsync(myAddress, "tpcPrepare", twoPhaseCommitSerializer.encode(prepare));
            for (Address address : otherAddresses) {
                ms.sendAsync(address, "tpcPrepare", twoPhaseCommitSerializer.encode(prepare));
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

            tpcLogger.updateServerLog(prepare, ServerLog.Status.PREPARED);

            ms.sendAsync(Address.from(a.host(), a.port()), "tpcResponseOk", twoPhaseCommitSerializer.encode(prepare));
        }, executor);

        ms.registerHandler("tpcResponseOk", (a, b) -> {
            System.out.println("tpcResponseOk");
            TwoPhaseCommit response = twoPhaseCommitSerializer.decode(b);

            boolean allAccepted = tpcLogger.updateCoordinatorLog(response, CoordinatorLog.Status.COMMITED);
            if (allAccepted) {
                ms.sendAsync(myAddress, "tpcCommit", twoPhaseCommitSerializer.encode(response));
                for (Address address : otherAddresses) {
                    ms.sendAsync(address, "tpcCommit", twoPhaseCommitSerializer.encode(response));
                }
            }
        }, executor);

        ms.registerHandler("tpcResponseNotOk", (a, b) -> {
            System.out.println("tpcResponseNotOk");
            TwoPhaseCommit response = twoPhaseCommitSerializer.decode(b);

            tpcLogger.updateCoordinatorLog(response, CoordinatorLog.Status.ABORTED);

            ms.sendAsync(myAddress, "tpcRollback", twoPhaseCommitSerializer.encode(response));
            for (Address address : otherAddresses) {
                ms.sendAsync(address, "tpcRollback", twoPhaseCommitSerializer.encode(response));
            }
        }, executor);

        ms.registerHandler("tpcCommit", (a, b) -> {
            System.out.println("tpcCommit");
            TwoPhaseCommit commit = twoPhaseCommitSerializer.decode(b);

            tpcLogger.updateServerLog(commit, ServerLog.Status.COMMITED);

            if (commit.getTweet() != null)
                dbHandler.addTweet(commit.getTweet());
            else
                dbHandler.updateSubscriptions(commit.getUsername(), commit.getTopics());
        }, executor);

        ms.registerHandler("tpcRollback", (a, b) -> {
            System.out.println("tpcRollback");
            TwoPhaseCommit rollback = twoPhaseCommitSerializer.decode(b);

            tpcLogger.updateServerLog(rollback, ServerLog.Status.ABORTED);
        }, executor);
    }
}
