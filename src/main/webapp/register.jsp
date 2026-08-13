<% String pageTitle = "Register Your Shop"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container-narrow">
    <h1 style="font-size: 1.6rem;">Register your shop</h1>
    <p class="text-muted">Set up a virtual queue for your clinic, salon, or shop in under a minute.</p>

    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="card">
        <form action="${pageContext.request.contextPath}/register" method="post">
            <div class="field">
                <label for="shopName">Shop name</label>
                <input type="text" id="shopName" name="shopName" placeholder="e.g. Sunrise Family Clinic" required>
            </div>

            <div class="field">
                <label for="category">Category</label>
                <select id="category" name="category">
                    <option value="Clinic">Clinic</option>
                    <option value="Salon">Salon / Barbershop</option>
                    <option value="Repair Shop">Repair Shop</option>
                    <option value="Government Office">Government Office</option>
                    <option value="General">Other</option>
                </select>
            </div>

            <div class="field">
                <label for="username">Login username</label>
                <input type="text" id="username" name="username" placeholder="Used to log in to your dashboard" required>
            </div>

            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" minlength="6" required>
                <div class="field-hint">At least 6 characters.</div>
            </div>

            <div class="field">
                <label for="avgServiceTime">Average time per customer (minutes)</label>
                <input type="number" id="avgServiceTime" name="avgServiceTime" min="1" max="240" value="10" required>
                <div class="field-hint">Used to estimate customers' wait times. You can change this anytime.</div>
            </div>

            <button type="submit" class="btn btn-amber btn-block">Create shop account</button>
        </form>
    </div>

    <p class="text-center text-muted mt-24">
        Already registered? <a href="${pageContext.request.contextPath}/login.jsp">Log in</a>
    </p>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
