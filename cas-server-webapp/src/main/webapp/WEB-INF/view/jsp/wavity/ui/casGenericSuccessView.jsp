<%-- <jsp:directive.include file="includes/login_top.jsp" />
	<div style="background-color:#333;">
		<div id="msg" class="success">
			<h2><spring:message code="screen.success.header" /></h2>
			<p><spring:message code="screen.success.success" arguments="${principal.id}"/></p>
			<p><spring:message code="screen.success.security" /></p>
		</div>
		<div><a href="/cas/logout">Log out</a></div>
	</div>
<jsp:directive.include file="includes/login_bottom.jsp" /> --%>

<!DOCTYPE html>

<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html lang="en">
	<head>
		<meta charset="UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<title>Wavity Login</title>
		<meta name="viewport" content="width=device-width, initial-scale=1">

		<spring:theme code="standard.login.css.bootstrap" var="loginCssBootstrap" />
	    <spring:theme code="standard.login.css.form" var="loginCssForm" />
	    <spring:theme code="standard.login.css.animate" var="loginCssAnimate" />
	    <spring:theme code="standard.login.css.login" var="loginCssLogin" />
	    <spring:theme code="standard.login.css.stickyFooter" var="loginCssStickyFooter" />
		<link rel="stylesheet" href="<c:url value="${loginCssBootstrap}" />" />
		<link rel="stylesheet" href="<c:url value="${loginCssForm}" />" />
		<link rel="stylesheet" href="<c:url value="${loginCssAnimate}" />" />
		<link rel="stylesheet" href="<c:url value="${loginCssLogin}" />" />
		<link rel="stylesheet" href="<c:url value="${loginCssStickyFooter}" />" />
		
		<spring:theme code="cas.login.javascript.require" var="loginJsRequire" />
		<script type="text/javascript" src="<c:url value="${loginJsRequire}" />"></script>
	</head>
	<body role="application" class="bodyLayout">
		<header role="banner" id="ot-header" class="header">
			<!-- header region -->
		</header>
		<main role="main" id="ot-main" class="main">
			<section id="loginColumns" class="animated fadeInDown">
				<div class="row">		
					<div class="col-md-6 hidden-xs">			
						<img id="domainIcon" width="400px" height="400px" class="m-t-50" src="themes/wavity/res/lib/custom/img/LogInScreen/LogInScreen_large_background_logo_oneteam.png"/>
					</div>
					<div class="col-md-6">
						<div style="color:#333;">
							<div id="msg" class="success">
								<h2><spring:message code="screen.success.header" /></h2>
								<p><spring:message code="screen.success.success" arguments="${principal.id}"/></p>
								<p><spring:message code="screen.success.security" /></p>
							</div>
						</div>
					</div>
				</div>
			</section>
		</main>
		<footer role="contentinfo" id="ot-footer" class="footer">
			<!-- footer region -->
		</footer>
	</body>
</html>