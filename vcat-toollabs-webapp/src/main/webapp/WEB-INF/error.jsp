<!DOCTYPE html>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<html lang="en">
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
					<h2>
						Error/<span lang="de">Fehler</span>
					</h2>
					<p>
						More information on vCat at <a
							href="//meta.wikimedia.org/wiki/User:Dapete/vCat#English">User:Dapete/vCat</a>
						(Meta). Report problems at <a
							href="//meta.wikimedia.org/wiki/User_talk:Dapete/vCat">User
							talk:Dapete/vCat</a>.
					</p>
					<p lang="de">
						Mehr Informationen über vCat unter <a
							href="//meta.wikimedia.org/wiki/User:Dapete/vCat#Deutsch">User:Dapete/vCat</a>
						(Meta). Probleme melden unter <a
							href="//meta.wikimedia.org/wiki/User_talk:Dapete/vCat">User
							talk:Dapete/vCat</a>.
					</p>
					<p class="error"><%=StringEscapeUtils.escapeXml10((String) request.getAttribute("exceptionMessage"))%></p>
					<pre><%=StringEscapeUtils.escapeXml10((String) request.getAttribute("stacktrace"))%></pre>
				</div>
			</div>
			<div class="col2">

				<div id="logo">
					<div>
						<a href="//tools.wmflabs.org/"><img
							src="//tools.wmflabs.org/Tool_Labs_logo_thumb.png"
							alt="Wikitech and Wikimedia Labs" height="138" width="122" /></a>
					</div>
					<div>
						<a href="./"><img src="res/vCat_logo_thumb.png" alt="vCat" /></a>
					</div>
				</div>

				<strong>Links</strong>
				<ul>
					<li><a href="//meta.wikimedia.org/wiki/User:Dapete/vCat">User:Dapete/vCat</a>
						(Meta)</li>
					<li>GitHub: <a href="https://github.com/dapete42/vcat">dapete42/vcat</a>,
						<a href="https://github.com/dapete42/vcat-deployed">dapete42/vcat-deployed</a>
				</ul>

			</div>
		</div>
	</div>
</body>
</html>
