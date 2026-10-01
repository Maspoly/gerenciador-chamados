<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>

<!DOCTYPE html>
<html lang="pt-BR">

<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<body data-page="admin-areas-comuns">

<div class="app-shell">

    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>

    <div class="app-main">

        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>

        <main class="page-content">

            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <section class="two-column-grid">



                <section class="card">

                    <div class="section-header">

                        <div>

                            <span class="eyebrow">
                                Áreas Comuns
                            </span>

                            <c:choose>

                                <c:when test="${not empty areaEmEdicao}">
                                    <h2>Editar área</h2>
                                </c:when>

                                <c:otherwise>
                                    <h2>Cadastrar área</h2>
                                </c:otherwise>

                            </c:choose>

                        </div>

                    </div>



                    <c:choose>

                        <c:when test="${not empty areaEmEdicao}">
                            <c:set
                                var="areaFormAction"
                                value="${ctx}/admin/areas-comuns/${areaEmEdicao.id}"
                            />
                        </c:when>

                        <c:otherwise>
                            <c:set
                                var="areaFormAction"
                                value="${ctx}/admin/areas-comuns"
                            />
                        </c:otherwise>

                    </c:choose>


                    <form
                        method="post"
                        action="${areaFormAction}"
                        class="stack-form"
                    >

                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>



                        <c:if test="${not empty areaEmEdicao}">

                            <input
                                type="hidden"
                                name="_method"
                                value="patch"
                            >

                        </c:if>


                        <div class="field">

                            <label for="nome">
                                Nome
                            </label>

                            <input
                                type="text"
                                id="nome"
                                name="nome"
                                maxlength="255"
                                value="${fn:escapeXml(areaEmEdicao.nome)}"
                                required
                            >

                        </div>


                        <div class="field">

                            <label for="descricao">
                                Descrição
                            </label>

                            <textarea
                                id="descricao"
                                name="descricao"
                                maxlength="500"
                                rows="4"
                            ><c:out value="${areaEmEdicao.descricao}" /></textarea>

                        </div>


                        <div class="button-row">

                            <c:choose>

                                <c:when test="${not empty areaEmEdicao}">

                                    <button
                                        type="submit"
                                        class="btn btn-primary"
                                    >
                                        Salvar alterações
                                    </button>


                                    <a
                                        href="${ctx}/admin/areas-comuns"
                                        class="btn btn-secondary"
                                    >
                                        Cancelar edição
                                    </a>

                                </c:when>


                                <c:otherwise>

                                    <button
                                        type="submit"
                                        class="btn btn-primary"
                                    >
                                        Cadastrar
                                    </button>

                                </c:otherwise>

                            </c:choose>

                        </div>

                    </form>

                </section>



                <section class="card">

                    <div class="section-header">

                        <div>

                            <span class="eyebrow">
                                Cadastro
                            </span>

                            <h2>
                                Áreas cadastradas
                            </h2>

                        </div>

                    </div>


                    <div class="table-wrap">

                        <table class="data-table">

                            <thead>

                                <tr>

                                    <th>Nome</th>

                                    <th>Descrição</th>

                                    <th>Status</th>

                                    <th>Ações</th>

                                </tr>

                            </thead>


                            <tbody>

                                <c:forEach
                                    var="area"
                                    items="${areas}"
                                >

                                    <tr>

                                        <td>
                                            <c:out value="${area.nome}" />
                                        </td>


                                        <td>

                                            <c:choose>

                                                <c:when test="${not empty area.descricao}">
                                                    <c:out value="${area.descricao}" />
                                                </c:when>

                                                <c:otherwise>
                                                    -
                                                </c:otherwise>

                                            </c:choose>

                                        </td>


                                        <td>

                                            <c:choose>

                                                <c:when test="${area.ativa}">

                                                    <span class="status-pill">
                                                        Ativa
                                                    </span>

                                                </c:when>


                                                <c:otherwise>

                                                    <span class="status-pill neutral">
                                                        Inativa
                                                    </span>

                                                </c:otherwise>

                                            </c:choose>

                                        </td>


                                        <td>

                                            <div class="toolbar-inline">



                                                <a
                                                    href="${ctx}/admin/areas-comuns?editar=${area.id}"
                                                    class="btn btn-secondary"
                                                >
                                                    Editar
                                                </a>



                                                <c:if test="${area.ativa}">

                                                    <form
                                                        method="post"
                                                        action="${ctx}/admin/areas-comuns/${area.id}/desativar"
                                                    >

                                                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

                                                        <input
                                                            type="hidden"
                                                            name="_method"
                                                            value="patch"
                                                        >

                                                        <button
                                                            type="submit"
                                                            class="btn btn-secondary"
                                                        >
                                                            Desativar
                                                        </button>

                                                    </form>

                                                </c:if>



                                                <c:if test="${not area.ativa}">

                                                    <form
                                                        method="post"
                                                        action="${ctx}/admin/areas-comuns/${area.id}/reativar"
                                                    >

                                                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

                                                        <input
                                                            type="hidden"
                                                            name="_method"
                                                            value="patch"
                                                        >

                                                        <button
                                                            type="submit"
                                                            class="btn btn-secondary"
                                                        >
                                                            Reativar
                                                        </button>

                                                    </form>

                                                </c:if>

                                            </div>

                                        </td>

                                    </tr>

                                </c:forEach>


                                <c:if test="${empty areas}">

                                    <tr>

                                        <td colspan="4">

                                            <div class="empty-state">

                                                Nenhuma área comum cadastrada.

                                            </div>

                                        </td>

                                    </tr>

                                </c:if>

                            </tbody>

                        </table>

                    </div>

                </section>

            </section>

        </main>

    </div>

</div>


<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>

</body>

</html>