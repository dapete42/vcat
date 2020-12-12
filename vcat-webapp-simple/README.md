vcat-webapp-simple
==================

Tomcat encoding problems
------------------------

Tomcat assumes URIs are encoded as ISO-8859-1; however, these days all
browsers send UTF-8, so passing any parameters with non-ASCII characters will
fail. To change this, add
	URIEncoding="UTF-8"
to the <Connector> definition(s) in your Tomcat's server.xml.

Configuration
-------------

The configuration file src/main/resources/vcat/servlet/config.properties
contains only a single setting, cachedir. This can be used to set where the
cached files (API access, metadata, Graphviz and rendered images) are stored.
