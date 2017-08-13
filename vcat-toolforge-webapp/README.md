vcat-toolforge-webapp
====================

Tomcat encoding problems
------------------------

Tomcat assumes URIs are encoded as ISO-8859-1; however, these days all
browsers send UTF-8, so passing any parameters with non-ASCII characters will
fail. To change this, add
	URIEncoding="UTF-8"
to the <Connector> definition(s) in your Tomcat's server.xml.

Running with Jetty
------------------

Jetty is a simple HTTP server with servlet support. To run this application
with it, use
	mvn jetty:run
on the command line or otherwise start Maven's 'jetty:run' goal. I set this up
primarily for testing and debugging, but it should work even in a production
environment. The web server does not have much to do, anyway.
