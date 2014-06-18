vcat-webapp
===========

Tomcat encoding problems
------------------------

Tomcat assumes URIs are encoded as ISO-8859-1; however, these days all
browsers send UTF-8, so passing any parameters with non-ASCII characters will
fail. To change this, add
	URIEncoding="UTF-8"
to the <Connector> definition(s) in your Tomcat's server.xml.
