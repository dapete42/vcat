vcat-toollabs
=============

Configuration file
------------------

The finished program needs a configuration file. This is an example of the
properties that need to be set in this file:

	# Cache directory for Graphviz and image files
	cache.dir=./cache
	
	# Purge caches after (seconds)
	purge=600
	# Purge metadata after (seconds)
	purge.metadata=86400
	
	# Redis server information
	redis.server.hostname=localhost
	#redis.server.port=
	#redis.server.password=
	
	# Redis secret used for prefix
	redis.secret=1234567890
	
	# Redis channel suffixes
	redis.channel.request.suffix=-requests
	redis.channel.response.suffix=-responses
	
	# Redis key suffixes
	redis.key.request.suffix=-request
	redis.key.response.suffix=-response
	redis.key.response.error.suffix=-response-error
	redis.key.response.headers.suffix=-response-headers
