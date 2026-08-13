<% String pageTitle = "Shop Login"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container-narrow">
    <h1 style="font-size: 1.6rem;">Shop login</h1>
    <p class="text-muted">Manage your queue and call the next customer.</p>

    <c:if test="${param.registered == 'true'}">
        <div class="alert alert-success">Account created. You can log in now.</div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="card">
        <form action="${pageContext.request.contextPath}/login" method="post">
            <div class="field">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" required autofocus>
            </div>
            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" required>
            </div>
            <button type="submit" class="btn btn-amber btn-block">Log in</button>
        </form>
    </div>

    <p class="text-center text-muted mt-24">
        No shop account yet? <a href="${pageContext.request.contextPath}/register.jsp">Register your shop</a>
    </p>
    <p class="text-center text-muted" style="font-size: 0.82rem;">
        Demo login &mdash; username: <code>demo_clinic</code>, password: <code>demo123</code>
    </p>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
