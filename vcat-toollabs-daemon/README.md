vcat-toollabs
=============

Configuration file
------------------

The finished program needs a configuration file. This is an example of the
properties that need to be set in this file (default values are commented
out):

	# URL of the 'render' script
	render.url=http://tools.wmflabs.org/vcat/render
	
	# Cache directory for Graphviz and image files
	cache.dir=/tmp/vcat-cache
	
	# Temporary directory for Graphviz and image files
	temp.dir=/tmp/vcat-temp
	
	# Directory with Graphviz binaries (dot, fdp)
	#graphviz.dir=/usr/bin
	# Maximum number of concurrent processes running Graphviz (0=unlimited)
	#graphviz.processes=0
	
	# Purge caches after (seconds)
	#purge=600
	# Purge metadata after (seconds)
	#purge.metadata=86400
	
	# JDBC URL for MySQL/MariaDB access to wiki table
	jdbc.url=jdbc:mysql://enwiki.labsdb:3306/meta_p
	# Username and password are read from replica.my.cnf in user's home directory
	
	# Redis server information
	redis.server.hostname=localhost
	#redis.server.port=6379
	
	# Redis secret used for prefix
	redis.secret=1234567890
	
	# Redis channel suffixes
	redis.channel.control.suffix=-control
	redis.channel.request.suffix=-requests
	redis.channel.response.suffix=-responses

The name of this configuration file must passed as the first command line
parameter when running the program.
