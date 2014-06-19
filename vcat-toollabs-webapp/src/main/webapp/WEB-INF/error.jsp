<!DOCTYPE html>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link rel="stylesheet" type="text/css"
	href="//tools.wmflabs.org/style.css" />
<link rel="stylesheet" type="text/css" href="res/vcat.css" />
<title>vCat</title>
</head>
<body>
	<div class="colmask leftmenu">
		<div class="colright">
			<div class="col1wrap">
				<div class="col1">
					<header>
						<h1>vCat</h1>
					</header>
					<h2>Error</h2>
					<p class="error"><%=StringEscapeUtils.escapeXml10((String) request.getAttribute("exceptionMessage"))%></p>
					<pre><%=StringEscapeUtils.escapeXml10((String) request.getAttribute("stacktrace"))%></pre>
					<p>
						More information on vCat at <a
							href="//meta.wikimedia.org/wiki/User:Dapete/vCat">User:Dapete/vCat</a>
						(Meta)
					</p>
				</div>
			</div>
			<div class="col2">
				<div id="logo">
					<a href="//tools.wmflabs.org/"> <img
						src="//tools.wmflabs.org/Tool_Labs_logo_thumb.png"
						alt="Wikitech and Wikimedia Labs" height="138" width="122"></a>
				</div>
			</div>
		</div>
	</div>
</body>
</html>
