<c:set var="app_version" value="${pom.version}"/>
<c:set var="app_mainStyleSheet" value="${app.mainStyleSheet}"/>

<c:if test="${app_version}">
	<c:set var="app_version" value="X.X.X.X"/>	
</c:if>
<c:if test="${app_mainStyleSheet}">
	<c:set var="app_mainStyleSheet" value="app_global.css"/>	
</c:if>