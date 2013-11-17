package vcat.redis;

import redis.clients.jedis.JedisPubSub;

/**
 * Simple subclass of {@link JedisPubSub}. Implements all necessary methods as dummy methods, so only those needed can
 * be overwritten.
 * 
 * @author Peter Schlömer *
 */
public abstract class SimplePubSub extends JedisPubSub {

	@Override
	public void onMessage(String channel, String message) {
	}

	@Override
	public void onPMessage(String pattern, String channel, String message) {
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
	}

	@Override
	public void onPUnsubscribe(String pattern, int subscribedChannels) {
	}

	@Override
	public void onPSubscribe(String pattern, int subscribedChannels) {
	}

}
