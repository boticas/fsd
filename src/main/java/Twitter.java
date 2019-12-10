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

        // Extract the addresses of the servers
        Address myAddress = Address.from(args[0]);
        ArrayList<Address> allAddresses = new ArrayList<Address>(args.length);
        for (int i = 0; i < args.length; i++)
            allAddresses.add(Address.from(args[i]));

        // Setup the infrastructure related to DB managment
        DBHandler dbHandler = new DBHandler(myAddress.port());

        // Get the server ready for receiving messages from its peers
        ExecutorService executor = Executors.newFixedThreadPool(1);

        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());
        ms.start();

        // Serializer for all the messages
        Serializer serializer = new SerializerBuilder().addType(Tweet.class).addType(Tweets.class)
                .addType(SubscribeTopics.class).addType(GetTweets.class).addType(GetTopics.class)
                .addType(Response.class).addType(TwoPhaseCommit.class).build();

        // Setup the infrastructure related to TPC and respective logs
        TPCHandler tpcHandler = new TPCHandler(allAddresses, myAddress.port(), serializer, ms);

        ms.registerHandler("publishTweet", (a, b) -> {
            System.out.println("publishTweet");
            Tweet newTweet = serializer.decode(b);
            newTweet.orderTopics();

            TwoPhaseCommit prepare = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter(), newTweet);

            tpcHandler.updateCoordinatorLog(prepare, CoordinatorLog.Status.STARTED, myAddress);

            for (Address address : allAddresses) {
                ms.sendAsync(address, "tpcPrepare", serializer.encode(prepare));
            }
        }, executor);

        ms.registerHandler("subscribeTopics", (a, b) -> {
            System.out.println("subscribeTopics");
            SubscribeTopics st = serializer.decode(b);

            TwoPhaseCommit prepare = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter(), st.getUsername(),
                    st.getTopics());

            tpcHandler.updateCoordinatorLog(prepare, CoordinatorLog.Status.STARTED, myAddress);

            for (Address address : allAddresses) {
                ms.sendAsync(address, "tpcPrepare", serializer.encode(prepare));
            }
        }, executor);

        ms.registerHandler("getTweets", (a, b) -> {
            System.out.println("getTweets");
            GetTweets gt = serializer.decode(b);
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
            GetTopics gt = serializer.decode(b);
            String username = gt.getUsername();
            // Return the topics username is subscribed to
        }, executor);

        ms.registerHandler("tpcPrepare", (a, b) -> {
            System.out.println("tpcPrepare");
            TwoPhaseCommit prepare = serializer.decode(b);

            tpcHandler.updateServerLog(prepare, ServerLog.Status.PREPARED, Address.from(a.host(), a.port()));

            ms.sendAsync(Address.from(a.host(), a.port()), "tpcResponseOk", serializer.encode(prepare));
        }, executor);

        ms.registerHandler("tpcResponseOk", (a, b) -> {
            System.out.println("tpcResponseOk");
            TwoPhaseCommit response = serializer.decode(b);

            boolean allAccepted = tpcHandler.updateCoordinatorLog(response, CoordinatorLog.Status.COMMITED);
            if (allAccepted) {
                for (Address address : allAddresses)
                    ms.sendAsync(address, "tpcCommit", serializer.encode(response));
            }
        }, executor);

        ms.registerHandler("tpcResponseNotOk", (a, b) -> {
            System.out.println("tpcResponseNotOk");
            TwoPhaseCommit response = serializer.decode(b);

            tpcHandler.updateCoordinatorLog(response, CoordinatorLog.Status.ABORTED);

            for (Address address : allAddresses) {
                ms.sendAsync(address, "tpcRollback", serializer.encode(response));
            }
        }, executor);

        ms.registerHandler("tpcCommit", (a, b) -> {
            System.out.println("tpcCommit");
            TwoPhaseCommit commit = serializer.decode(b);

            ArrayList<TwoPhaseCommit> tpcsToApply = tpcHandler.updateServerLog(commit, ServerLog.Status.COMMITED,
                    Address.from(a.host(), a.port()));
            for (TwoPhaseCommit tpc : tpcsToApply) {
                if (tpc.getTweet() != null)
                    dbHandler.addTweet(tpc.getTweet());
                else
                    dbHandler.updateSubscriptions(tpc.getUsername(), tpc.getTopics());
            }
        }, executor);

        ms.registerHandler("tpcRollback", (a, b) -> {
            System.out.println("tpcRollback");
            TwoPhaseCommit rollback = serializer.decode(b);

            ArrayList<TwoPhaseCommit> tpcsToApply = tpcHandler.updateServerLog(rollback, ServerLog.Status.ABORTED,
                    Address.from(a.host(), a.port()));
        }, executor);

        ms.registerHandler("tpcHeartbeat", (a, b) -> {
            System.out.println("tpcHeartbeat");
            TwoPhaseCommit heartbeat = serializer.decode(b);

            ArrayList<TwoPhaseCommit> tpcsToApply = tpcHandler.processHeartbeat(heartbeat,
                    Address.from(a.host(), a.port()));
            for (TwoPhaseCommit tpc : tpcsToApply) {
                if (tpc.getTweet() != null)
                    dbHandler.addTweet(tpc.getTweet());
                else
                    dbHandler.updateSubscriptions(tpc.getUsername(), tpc.getTopics());
            }
        }, executor);

        ms.registerHandler("tpcGetHeartbeat", (a, b) -> {
            System.out.println("tpcGetHeartbeat");
            TwoPhaseCommit heartbeat = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter());
            ms.sendAsync(Address.from(a.host(), a.port()), "tpcHeartbeat", serializer.encode(heartbeat));
        }, executor);
    }
}
