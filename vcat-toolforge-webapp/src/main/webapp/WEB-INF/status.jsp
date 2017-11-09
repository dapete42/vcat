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
	final long mb = 1024 * 1024;
	final long freeMemory = Runtime.getRuntime().freeMemory();
	final long maxMemory = Runtime.getRuntime().maxMemory();
	final long totalMemory = Runtime.getRuntime().totalMemory();
%>
<html lang="en">
<head>
<meta charset="utf-8" />
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

					<table class="status">
						<caption>System properties</caption>
						<tbody>
							<tr>
								<th scope="col">JRE vendor/version</th>
								<td><%=System.getProperty("java.vendor")%>, <%=System.getProperty("java.version")%></td>
							</tr>
							<tr>
								<th scope="col">Operating system architecture</th>
								<td><%=System.getProperty("os.arch")%></td>
							</tr>
							<tr>
								<th scope="col">Operation system name/version</th>
								<td><%=System.getProperty("os.name")%>, <%=System.getProperty("os.version")%></td>
							</tr>
							<tr>
								<th scope="col">User name</th>
								<td><%=System.getProperty("user.name")%></td>
							</tr>
						</tbody>
					</table>

					<table class="status">
						<caption>Server properties</caption>
						<tbody>
							<tr>
								<th scope="col">Server info</th>
								<td><%=getServletContext().getServerInfo()%></td>
							</tr>
						</tbody>
					</table>

					<table class="status">
						<caption>Memory</caption>
						<tbody>
							<tr>
								<th scope="col">Allocated memory</th>
								<td class="number"><%=totalMemory / mb%> MB</td>
							</tr>
							<tr>
								<th scope="col">Free allocated memory</th>
								<td class="number"><%=freeMemory / mb%> MB</td>
							</tr>
							<tr>
								<th scope="col">Maximum memory</th>
								<td class="number"><%=maxMemory / mb%> MB</td>
							</tr>
							<tr>
								<th scope="col">Free total memory</th>
								<td class="number"><%=(freeMemory + maxMemory - totalMemory) / mb%>
									MB</td>
							</tr>
						</tbody>
					</table>

					<table class="status">
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
							src="//tools-static.wmflabs.org/toolforge/logos/logo-with-text.png"
							alt="Wikimedia Toolforge" height="125" width="125" /></a>
					</div>
					<div>
						<a href="./"><img src="res/vcat.png" alt="vCat" /></a>
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
