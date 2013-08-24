package vcat.toollabs;

import java.io.File;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import vcat.VCatException;
import vcat.VCatRenderer;
import vcat.VCatRenderer.RenderedFileInfo;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.graphviz.GraphvizJNI;

public class Main {

	private static final String CACHE_DIR = "./cache";

	private static final String CHANNEL_REQUEST = "vcat-requests";

	private static final String CHANNEL_RESPONSE = "vcat-responses";

	private static final String SUFFIX_REQUEST = "-request";

	private static final String SUFFIX_RESPONSE = "-response";

	private static final String SUFFIX_RESPONSE_ERROR = "-response-error";

	private static final String SUFFIX_RESPONSE_HEADERS = "-response-headers";

	public static void main(String[] args) throws GraphvizException, VCatException {

		// Cache is in cache subdirectory of working directory
		final File tmpDir = new File(CACHE_DIR);

		final Jedis jedis = new Jedis("localhost");
		final Jedis jedisListen = new Jedis("localhost");

		// Call Graphviz using JNI
		final Graphviz graphviz = new GraphvizJNI();
		// Create renderer
		final VCatRenderer vCatRenderer = new VCatRenderer(tmpDir, graphviz);

		JedisPubSub jedisSubscribe = new JedisPubSub() {

			@Override
			public void onMessage(String channel, String message) {
				renderJson(jedis, message, vCatRenderer);
			}

			@Override
			public void onSubscribe(String channel, int subscribedChannels) {
			}

			@Override
			public void onPUnsubscribe(String pattern, int subscribedChannels) {
			}

			@Override
			public void onPMessage(String pattern, String channel, String message) {
			}

			@Override
			public void onPSubscribe(String pattern, int subscribedChannels) {
			}

			@Override
			public void onUnsubscribe(String channel, int subscribedChannels) {
			}
		};

		jedisListen.subscribe(jedisSubscribe, CHANNEL_REQUEST);

	}

	private static void fillParametersFromJson(final HashMap<String, String[]> parameterMap, final String jsonString)
			throws VCatException {
		try {
			final JSONObject json = new JSONObject(new JSONTokener(jsonString));
			JSONArray jsonArrayNames = json.names();
			for (int i = 0; i < jsonArrayNames.length(); i++) {
				final String jsonArrayName = jsonArrayNames.getString(i);
				final JSONArray jsonArray = json.getJSONArray(jsonArrayName);
				final String[] parameterArray = new String[jsonArray.length()];
				for (int j = 0; j < jsonArray.length(); j++) {
					parameterArray[j] = jsonArray.getString(j);
				}
				parameterMap.put(jsonArrayName, parameterArray);
			}
		} catch (JSONException e) {
			throw new VCatException("Error parsing json", e);
		}
	}

	private static void handleError(final Jedis jedis, final String jedisKey, final Exception e) {
		final String errorMessage = e.getMessage();
		System.err.println(errorMessage);
		e.printStackTrace();
		jedis.set(jedisKey + SUFFIX_RESPONSE_ERROR, errorMessage);
		jedis.expire(jedisKey, 60);
		jedis.publish(CHANNEL_RESPONSE, jedisKey);
		jedis.del(jedisKey + SUFFIX_REQUEST);
	}

	protected static void renderJson(final Jedis jedis, final String jedisKey, final VCatRenderer vCatRenderer) {

		final String jsonRequestKey = jedisKey + SUFFIX_REQUEST;
		final String jsonResponseHeadersKey = jedisKey + SUFFIX_RESPONSE_HEADERS;
		final String responseKey = jedisKey + SUFFIX_RESPONSE;

		final HashMap<String, String[]> parameterMap = new HashMap<String, String[]>();
		try {
			fillParametersFromJson(parameterMap, jedis.get(jsonRequestKey));
		} catch (VCatException e) {
			handleError(jedis, jedisKey, e);
			return;
		}

		new Thread() {

			@Override
			public void run() {
				super.run();
				RenderedFileInfo renderedFileInfo;
				try {
					renderedFileInfo = vCatRenderer.render(parameterMap);
				} catch (VCatException e) {
					handleError(jedis, jedisKey, e);
					jedis.del(jsonRequestKey);
					return;
				}

				JSONObject json = new JSONObject();
				try {
					// Content-type, as returned from rendering process
					String contentType = renderedFileInfo.getMimeType();
					json.put("Content-type", contentType);
					// Content-length, using length of temporary output file already written
					long length = renderedFileInfo.getFile().length();
					if (length < Integer.MAX_VALUE) {
						json.put("Content-length", Long.toString(length));
					}
					// Content-disposition, to determine filename of returned contents
					String filename = renderedFileInfo.getFile().getName();
					json.put("Content-disposition", "filename=\"" + filename + "\"");
				} catch (JSONException e) {
					// TODO handle this
					return;
				}

				// Set return values
				jedis.set(jsonResponseHeadersKey, json.toString());
				jedis.set(responseKey, renderedFileInfo.getFile().getAbsolutePath());
				// Values last for 60 seconds
				jedis.expire(jsonResponseHeadersKey, 60);
				jedis.expire(responseKey, 60);
				// Notify client that response is ready
				jedis.publish(CHANNEL_RESPONSE, jedisKey);

				// Clean up request
				jedis.del(jsonRequestKey);

				System.out.println("Headers submitted to redis");
				System.out.println("Results in " + renderedFileInfo.getFile().getAbsolutePath());
				System.out.println("Finished thread " + this.getName());
			}

			@Override
			public void start() {
				super.start();
				System.out.println("Started thread " + this.getName());
			}
		}.start();

	}

}
