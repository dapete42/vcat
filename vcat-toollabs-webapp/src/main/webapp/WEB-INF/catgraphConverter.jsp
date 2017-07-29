<!DOCTYPE html>
<%@page import="java.net.URLDecoder"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="vcat.toollabs.webapp.Messages"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%
	final String lang = (String) request.getAttribute("lang");

	Messages m = new Messages(lang);
	final String[] languages = Messages.getString("ToollabsCatgraphConverterServlet.Languages").split(",");
	final String[] languageNames = Messages.getString("ToollabsCatgraphConverterServlet.LanguageNames").split(
			",");
%>
<html lang="<%=StringEscapeUtils.escapeXml10(lang)%>">
<head>
<meta charset="utf-8" />
<title><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("title"))%></title>
<link rel="stylesheet" type="text/css"
	href="//tools.wmflabs.org/admin/assets/style.css" />
<link rel="stylesheet" type="text/css" href="res/vcat.css" />
</head>
<body>
	<div class="colmask leftmenu">
		<div class="colright">
			<div class="col1wrap">
				<div class="col1">

					<header>
						<h1><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("title"))%></h1>
					</header>

					<form id="convert" method="POST">
						<h2><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("heading_convert"))%></h2>
						<div class="formline">
							<label for="inputUrl"><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("label_url"))%></label>
						</div>
						<div class="formline">
							<input type="url" name="inputUrl"
								value="<%=StringEscapeUtils.escapeXml10((String) request.getAttribute("inputUrl"))%>"
								id="inputUrl" class="formfullwidth" />
						</div>
						<p><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("warning_url"))%></p>
						<div class="formline">
							<input type="hidden" name="lang"
								value="<%=StringEscapeUtils.escapeXml10(lang)%>" /> <input
								type="submit" name="doConvert"
								value="<%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("button_convert"))%>" />
						</div>
					</form>

					<%
						if ((Boolean) request.getAttribute("hasResult")) {
					%>
					<div id="result">
						<h2><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("heading_result"))%></h2>
						<table class="result">
							<tbody>
								<tr>
									<th scope=row><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("th_input_url"))%></th>
									<td><%=StringEscapeUtils.escapeXml10((String) request.getAttribute("inputUrl"))%></td>
								</tr>
								<tr>
									<th scope=row><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("th_input_parameters"))%></th>
									<td>
										<ul class="oneline">
											<%
												for (Entry<String, String[]> entry : ((Map<String, String[]>) request.getAttribute("inputParameters"))
															.entrySet()) {
														final String key = entry.getKey();
														for (String value : entry.getValue()) {
											%><li><%=StringEscapeUtils.escapeXml10(URLDecoder.decode(value == null ? key : key + '='
								+ value, "UTF8"))%></li>
											<%
												}
													}
											%>
										</ul>
									</td>
								</tr>
								<tr>
									<th scope=row><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("th_output_parameters"))%></th>
									<td>
										<ul class="oneline">
											<%
												for (String nameValueString : ((List<String>) request.getAttribute("outputParameterList"))) {
											%><li><%=StringEscapeUtils.escapeXml10(URLDecoder.decode(nameValueString, "UTF8"))%></li>
											<%
												}
											%>
										</ul>
									</td>
								</tr>
								<tr class="highlight">
									<th scope=row><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("th_output_url"))%></th>
									<td><a
										href="<%=StringEscapeUtils.escapeXml10((String) request.getAttribute("outputUrl"))%>"
										target="catgraphConvertOutput"><%=StringEscapeUtils.escapeXml10((String) request.getAttribute("outputUrl"))%></a></td>
								</tr>
							</tbody>
						</table>
					</div>
					<%
						}
					%>

				</div>
			</div>
			<div class="col2">

				<div id="logo">
					<div>
						<a href="//tools.wmflabs.org/"><img
							src="//tools.wmflabs.org/admin/assets/Tool_Labs_logo_thumb.png"
							alt="Wikitech and Wikimedia Labs" height="138" width="122" /></a>
					</div>
					<div>
						<a href="./"><img src="res/vCat_logo_thumb.png" alt="vCat" /></a>
					</div>
				</div>

				<strong><%=StringEscapeUtils.escapeXml10(m.getCatgraphConverterString("menu_languages"))%></strong>
				<ul>
					<%
						for (int i = 0; i < languages.length; i++) {
							if ("en".equals(languages[i])) {
					%>
					<li><a href="catgraphConvert"><%=StringEscapeUtils.escapeXml10(languageNames[i])%></a>
					</li>
					<%
						} else {
					%>
					<li><a
						href="catgraphConvert?lang=<%=StringEscapeUtils.escapeXml10(languages[i])%>"><%=StringEscapeUtils.escapeXml10(languageNames[i])%></a>
					</li>
					<%
						}
						}
					%>
				</ul>

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
