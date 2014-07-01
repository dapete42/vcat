<!DOCTYPE html>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%
	Thread[] threads = new Thread[Thread.activeCount()];
	int numberOfThreads = Thread.enumerate(threads);
	while (threads.length != numberOfThreads) {
		threads = new Thread[numberOfThreads];
		numberOfThreads = Thread.enumerate(threads);
	}
%>
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
					<h2>Status</h2>

					<table>
						<caption>Memory</caption>
						<tbody>
							<tr>
								<th scope="col">Free memory</th>
								<td class="number"><%=Runtime.getRuntime().freeMemory() / (1024 * 1024)%>
									M</td>
							</tr>
							<tr>
								<th scope="col">Total memory</th>
								<td class="number"><%=Runtime.getRuntime().totalMemory() / (1024 * 1024)%>
									M</td>
							</tr>
							<tr>
								<th scope="col">Maximum memory</th>
								<td class="number"><%=Runtime.getRuntime().maxMemory() / (1024 * 1024)%>
									M</td>
							</tr>
						</tbody>
					</table>

					<table>
						<caption>Threads</caption>
						<thead>
							<tr>
								<th>Id</th>
								<th>Name</th>
								<th>Priority</th>
								<th>State</th>
							</tr>
						</thead>
						<tbody>
							<%
								for (Thread thread : threads) {
							%>
							<tr>
								<td class="number"><%=thread.getId()%></td>
								<td><%=thread.getName()%></td>
								<td class="number"><%=thread.getPriority()%></td>
								<td><%=thread.getState().toString()%></td>
							</tr>
							<%
								}
							%>
						</tbody>
					</table>
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
					<li><a href="https://github.com/dapete42/vcat">dapete42/vcat</a>
						(GitHub)</li>
					<li><a href="https://github.com/dapete42/vcat-deployed">dapete42/vcat-deployed</a>
						(GitHub)</li>
				</ul>

			</div>
		</div>
	</div>
</body>
</html>
