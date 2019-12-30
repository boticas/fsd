import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

public class TwitterSerializer {
	public static Serializer serializer = new SerializerBuilder().addType(Tweet.class).addType(Topics.class).addType(Tweets.class)
			.addType(SubscribeTopics.class).addType(GetTweets.class).addType(GetTopics.class)
			.addType(Response.class).addType(TwoPhaseCommit.class).addType(Address.class).addType(Status.class)
			.addType(Log.Status.class).addType(ServerJoin.class).addType(ServerJoinResponse.class).build();
}
