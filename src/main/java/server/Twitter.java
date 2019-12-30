package server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import common.*;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;

public class Twitter {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.out.println("Indique o endereço deste servidor e, opcionalmente, o de outro (ip:porta)");
            System.exit(1);
        }

        // Extract the addresses of the servers
        Address myAddress = Address.from(args[0]);
        HashSet<Address> allAddresses;

        // Serializer for all the messages
        Serializer serializer = TwitterSerializer.serializer;
        // Initialize the messaging service
        ManagedMessagingService ms = new NettyMessagingService("twitter", myAddress, new MessagingConfig());

        // Setup the infrastructure related to DB managment
        DBHandler dbHandler;
        if (args.length == 2 && !DBHandler.checkDB(myAddress.port())) {
            AtomicBoolean ok = new AtomicBoolean(false);
            ServerJoinResponse sjr = new ServerJoinResponse();

            ms.registerHandler("serverJoinResult", (a, b) -> {
                ServerJoinResponse aux = serializer.decode(b);
                sjr.set(aux);

                synchronized (ok) {
                    ok.set(true);
                    ok.notify();
                }
            }, Executors.newFixedThreadPool(1));
            ms.start().get();

            ServerJoin sj = new ServerJoin(myAddress);
            ms.sendAsync(Address.from(args[1]), "serverJoin", serializer.encode(sj));

            synchronized (ok) {
                while (!ok.get())
                    ok.wait();
            }

            ms.stop().get();
            ms.unregisterHandler("serverJoinResult");

            dbHandler = new DBHandler(myAddress.port(), sjr.getServers(), sjr.getAllTweets(), sjr.getTweetsDB(),
                    sjr.getSubscriptionsDB());
            allAddresses = sjr.getServers();
        } else {
            dbHandler = new DBHandler(myAddress);
            allAddresses = dbHandler.getServers();
        }

        // Setup the infrastructure related to TPC and respective logs
        TPCHandler tpcHandler = new TPCHandler(allAddresses, myAddress.port(), serializer, ms, dbHandler);

        // Register the handlers for receiving messages
        registerHandlers(ms, Executors.newFixedThreadPool(1), serializer, myAddress, allAddresses, dbHandler,
                tpcHandler);

        // Apply the operations pending in the log
        tpcHandler.applyPendingLog();
    }

    private static void registerHandlers(ManagedMessagingService ms, ExecutorService executor, Serializer serializer,
            Address myAddress, HashSet<Address> allAddresses, DBHandler dbHandler, TPCHandler tpcHandler)
            throws Exception {
        ms.registerHandler("publishTweet", (a, b) -> {
            System.out.println("publishTweet");
            Tweet newTweet = serializer.decode(b);

            TwoPhaseCommit prepare = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter(), newTweet,
                    myAddress, a);

            tpcHandler.updateCoordinatorLog(prepare, Log.Status.PREPARE, null);

            for (Address address : allAddresses)
                ms.sendAsync(address, "tpcPrepare", serializer.encode(prepare));
        }, executor);

        ms.registerHandler("subscribeTopics", (a, b) -> {
            System.out.println("subscribeTopics");
            SubscribeTopics st = serializer.decode(b);

            TwoPhaseCommit prepare = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter(), st.getUsername(),
                    st.getTopics(), myAddress, a);

            tpcHandler.updateCoordinatorLog(prepare, Log.Status.PREPARE, null);

            for (Address address : allAddresses)
                ms.sendAsync(address, "tpcPrepare", serializer.encode(prepare));
        }, executor);

        ms.registerHandler("serverJoin", (a, b) -> {
            System.out.println("serverJoin");
            ServerJoin sj = serializer.decode(b);

            TwoPhaseCommit prepare = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter(), sj, myAddress,
                    a);

            tpcHandler.updateCoordinatorLog(prepare, Log.Status.PREPARE, null);

            for (Address address : allAddresses)
                ms.sendAsync(address, "tpcPrepare", serializer.encode(prepare));
        }, executor);

        ms.registerHandler("getTweets", (a, b) -> {
            System.out.println("getTweets");
            GetTweets gt = serializer.decode(b);
            String username = gt.getUsername();
            ArrayList<String> topics = gt.getTopics();
            if (topics == null) {
                // Return last 10 tweets from the user's subscriptions
                Tweets last10all = new Tweets(dbHandler.getLast10Tweets(username));
                return serializer.encode(last10all);
            } else {
                // Return last 10 tweets for each of the topics that the user is subscribed
                Tweets last10some = new Tweets(dbHandler.getLast10TweetsPerTopic(username, topics));
                return serializer.encode(last10some);
            }
        }, executor);

        ms.registerHandler("getTopics", (a, b) -> {
            System.out.println("getTopics");
            GetTopics gt = serializer.decode(b);
            String username = gt.getUsername();
            // Return the topics username is subscribed to
            Topics topics = new Topics(dbHandler.getTopics(username));
            return serializer.encode(topics);
        }, executor);

        ms.registerHandler("tpcPrepare", (a, b) -> {
            System.out.println("tpcPrepare");
            TwoPhaseCommit prepare = serializer.decode(b);

            tpcHandler.updateServerLog(prepare, Log.Status.PREPARE, a);

            ms.sendAsync(a, "tpcResponseOk", serializer.encode(prepare));
        }, executor);

        ms.registerHandler("tpcResponseOk", (a, b) -> {
            System.out.println("tpcResponseOk");
            TwoPhaseCommit response = serializer.decode(b);

            boolean allAccepted = tpcHandler.updateCoordinatorLog(response, Log.Status.COMMIT, a);
            if (allAccepted) {
                if (response.getServerJoin() == null) {
                    Response result = new Response(true);
                    ms.sendAsync(response.getRequester(), "result", serializer.encode(result));
                } else {
                    HashSet<Address> servers = new HashSet<>(allAddresses);
                    servers.add(response.getRequester());
                    ServerJoinResponse sjr = new ServerJoinResponse(servers, dbHandler.getAllTweets(),
                            dbHandler.getTweetsDB(), dbHandler.getSubscriptionsDB());
                    ms.sendAsync(response.getRequester(), "serverJoinResult", serializer.encode(sjr));
                }

                for (Address address : allAddresses)
                    ms.sendAsync(address, "tpcCommit", serializer.encode(response));
            }
        }, executor);

        ms.registerHandler("tpcResponseNotOk", (a, b) -> {
            System.out.println("tpcResponseNotOk");
            TwoPhaseCommit response = serializer.decode(b);

            tpcHandler.updateCoordinatorLog(response, Log.Status.ABORT, a);

            for (Address address : allAddresses)
                ms.sendAsync(address, "tpcRollback", serializer.encode(response));
            Response result = new Response(false);
            ms.sendAsync(response.getRequester(), "result", serializer.encode(result));
        }, executor);

        ms.registerHandler("tpcCommit", (a, b) -> {
            System.out.println("tpcCommit");
            TwoPhaseCommit commit = serializer.decode(b);

            tpcHandler.updateServerLog(commit, Log.Status.COMMIT, a);
        }, executor);

        ms.registerHandler("tpcRollback", (a, b) -> {
            System.out.println("tpcRollback");
            TwoPhaseCommit rollback = serializer.decode(b);

            tpcHandler.updateServerLog(rollback, Log.Status.ABORT, a);
        }, executor);

        ms.registerHandler("tpcHeartbeat", (a, b) -> {
            System.out.println("tpcHeartbeat");
            TwoPhaseCommit heartbeat = serializer.decode(b);

            tpcHandler.processHeartbeat(heartbeat, a);
        }, executor);

        ms.registerHandler("tpcGetHeartbeat", (a, b) -> {
            System.out.println("tpcGetHeartbeat");
            TwoPhaseCommit heartbeat = new TwoPhaseCommit(tpcHandler.getAndIncrementTotalOrderCounter(), myAddress);
            ms.sendAsync(a, "tpcHeartbeat", serializer.encode(heartbeat));
        }, executor);

        ms.registerHandler("tpcGetStatus", (a, b) -> {
            System.out.println("tpcGetStatus");
            TwoPhaseCommit getStatus = serializer.decode(b);

            TwoPhaseCommit heartbeat = new TwoPhaseCommit(getStatus.getCount(), getStatus.getCoordinator());
            tpcHandler.processHeartbeat(heartbeat, a);

            Status status = tpcHandler.getStatus(getStatus, a);
            if (status.getStatus() != null)
                ms.sendAsync(a, "tpcStatus", serializer.encode(status));
        }, executor);

        ms.registerHandler("tpcStatus", (a, b) -> {
            System.out.println("tpcStatus");
            Status status = serializer.decode(b);

            tpcHandler.updateStatus(status);
        }, executor);

        ms.start().get();
    }
}
