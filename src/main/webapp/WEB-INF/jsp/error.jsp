<%@ page isErrorPage="true" %>
<% String pageTitle = "Something Went Wrong"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container-narrow text-center" style="padding-top: 40px;">
    <h1 style="font-size: 1.8rem;">Something went wrong</h1>
    <p class="text-muted">
        That page couldn't be found, or ran into an unexpected error.
        If you were in the middle of joining a queue, your token may still be safe -
        check your shop's page again.
    </p>
    <a href="${pageContext.request.contextPath}/index.jsp" class="btn btn-primary">Back to home</a>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
