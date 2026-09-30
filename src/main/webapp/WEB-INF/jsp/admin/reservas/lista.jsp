<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>

<!DOCTYPE html>

<html lang="pt-BR">

<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<body data-page="admin-reservas">

<div class="app-shell">

    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>

    <div class="app-main">

        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>

        <main class="page-content">

            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <article class="card">

                <div class="section-header">

                    <div>
                        <p class="eyebrow">Administracao</p>
                        <h2>Reservas</h2>
                    </div>

                </div>


                <%-- ========================= --%>
                <%-- CALENDARIO ADMIN          --%>
                <%-- ========================= --%>

                <div class="reservas-calendar-tools">

                    <label class="field">

                        <span>Area comum</span>

                        <select id="adminAreaComumId">

                            <option value="">
                                Selecione uma area
                            </option>

                            <c:forEach
                                var="area"
                                items="${areasCalendario}"
                            >

                                <option value="${area.id}">

                                    ${area.nome}

                                    <c:if test="${not area.ativa}">
                                        - Inativa
                                    </c:if>

                                </option>

                            </c:forEach>

                        </select>

                    </label>


                    <button
                        type="button"
                        class="btn btn-secondary"
                        id="abrirCalendarioAdminBtn"
                    >
                        Ver calendario
                    </button>

                </div>


                <%-- ========================= --%>
                <%-- FILTRO POR STATUS         --%>
                <%-- ========================= --%>

                <form
                    method="get"
                    action="${ctx}/admin/reservas"
                    class="filter-grid reservas-filter"
                >

                    <div class="field">

                        <label for="status">
                            Status
                        </label>

                        <select
                            id="status"
                            name="status"
                        >

                            <option value="">
                                Todas
                            </option>

                            <option
                                value="SOLICITADA"
                                ${statusSelecionado == 'SOLICITADA' ? 'selected' : ''}
                            >
                                Solicitadas
                            </option>

                            <option
                                value="APROVADA"
                                ${statusSelecionado == 'APROVADA' ? 'selected' : ''}
                            >
                                Aprovadas
                            </option>

                            <option
                                value="NEGADA"
                                ${statusSelecionado == 'NEGADA' ? 'selected' : ''}
                            >
                                Negadas
                            </option>

                            <option
                                value="CANCELADA"
                                ${statusSelecionado == 'CANCELADA' ? 'selected' : ''}
                            >
                                Canceladas
                            </option>

                        </select>

                    </div>


                    <div class="field">

                        <label>&nbsp;</label>

                        <button
                            type="submit"
                            class="btn btn-secondary"
                        >
                            Filtrar
                        </button>

                    </div>

                </form>


                <%-- ========================= --%>
                <%-- LISTAGEM                  --%>
                <%-- ========================= --%>

                <c:choose>

                    <c:when test="${empty reservas}">

                        <div class="empty-state">

                            <h3>
                                Nenhuma reserva encontrada
                            </h3>

                            <p>
                                Nao existem reservas para o filtro selecionado.
                            </p>

                        </div>

                    </c:when>


                    <c:otherwise>

                        <div class="table-wrap">

                            <table class="data-table reservas-table">

                                <thead>

                                    <tr>

                                        <th>Morador</th>

                                        <th>Area</th>

                                        <th>Inicio</th>

                                        <th>Fim</th>

                                        <th>Status</th>

                                        <th>Acoes</th>

                                    </tr>

                                </thead>


                                <tbody>

                                    <c:forEach
                                        var="reserva"
                                        items="${reservas}"
                                    >

                                        <tr>

                                            <td>
                                                ${reserva.moradorNome}
                                            </td>

                                            <td>
                                                ${reserva.areaComumNome}
                                            </td>

                                            <td>
                                                ${reserva.dataHoraInicioFormatada}
                                            </td>

                                            <td>
                                                ${reserva.dataHoraFimFormatada}
                                            </td>


                                            <%-- ========================= --%>
                                            <%-- STATUS                    --%>
                                            <%-- ========================= --%>

                                            <td>

                                                <span class="status-pill neutral">
                                                    ${reserva.status}
                                                </span>

                                                <c:if
                                                    test="${reserva.status == 'NEGADA' && not empty reserva.motivoNegativa}"
                                                >

                                                    <p class="reserva-motivo">

                                                        <strong>
                                                            Motivo:
                                                        </strong>

                                                        ${reserva.motivoNegativa}

                                                    </p>

                                                </c:if>

                                            </td>


                                            <%-- ========================= --%>
                                            <%-- ACOES                     --%>
                                            <%-- ========================= --%>

                                            <td class="cell-actions">

                                                <div class="reservation-actions">


                                                    <%-- SOLICITADA --%>

                                                    <c:if test="${reserva.status == 'SOLICITADA'}">

                                                        <form
                                                            method="post"
                                                            action="${ctx}/admin/reservas/${reserva.id}/aprovar"
                                                            class="reservation-action-form"
                                                        >

                                                            <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

                                                            <input
                                                                type="hidden"
                                                                name="_method"
                                                                value="patch"
                                                            >

                                                            <button
                                                                type="submit"
                                                                class="btn btn-primary"
                                                            >
                                                                Aprovar
                                                            </button>

                                                        </form>


                                                        <button
                                                            type="button"
                                                            class="btn btn-secondary js-negar-reserva"
                                                            data-reserva-id="${reserva.id}"
                                                        >
                                                            Negar
                                                        </button>
                                                        <button
                                                            type="button"
                                                            class="btn btn-secondary js-cancelar-reserva"
                                                            data-reserva-id="${reserva.id}"
                                                        >
                                                            Cancelar
                                                        </button>

                                                    </c:if>


                                                    <%-- APROVADA --%>

                                                    <c:if test="${reserva.status == 'APROVADA'}">

                                                        <button
                                                            type="button"
                                                            class="btn btn-secondary js-cancelar-reserva"
                                                            data-reserva-id="${reserva.id}"
                                                        >
                                                            Cancelar
                                                        </button>

                                                    </c:if>


                                                    <%-- SEM ACOES --%>

                                                    <c:if
                                                        test="${reserva.status == 'NEGADA' || reserva.status == 'CANCELADA'}"
                                                    >

                                                        <span>
                                                            -
                                                        </span>

                                                    </c:if>


                                                </div>

                                            </td>

                                        </tr>

                                    </c:forEach>

                                </tbody>

                            </table>

                        </div>

                    </c:otherwise>

                </c:choose>

            </article>

        </main>

    </div>

</div>


<%-- ========================= --%>
<%-- MODAL NEGAR               --%>
<%-- ========================= --%>

<dialog
    id="negarReservaModal"
    class="reserva-action-modal"
    data-action-base="${ctx}/admin/reservas"
>

    <form
        method="post"
        id="negarReservaForm"
        class="stack-form"
    >

        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

        <input
            type="hidden"
            name="_method"
            value="patch"
        >


        <div class="reserva-action-modal-header">

            <div>

                <p class="eyebrow">
                    Reserva
                </p>

                <h2>
                    Negar reserva
                </h2>

            </div>


            <button
                type="button"
                class="btn btn-secondary"
                id="fecharNegarReservaModal"
            >
                Fechar
            </button>

        </div>


        <div class="field">

            <label for="motivoNegativa">
                Motivo
            </label>

            <textarea
                id="motivoNegativa"
                name="motivo"
                maxlength="500"
                rows="4"
                required
                placeholder="Informe o motivo da negativa"
            ></textarea>

        </div>


        <div class="reserva-modal-actions">

            <button
                type="button"
                class="btn btn-secondary"
                id="cancelarNegativaBtn"
            >
                Voltar
            </button>


            <button
                type="submit"
                class="btn btn-primary"
            >
                Confirmar negativa
            </button>

        </div>

    </form>

</dialog>


<%-- ========================= --%>
<%-- MODAL CANCELAR            --%>
<%-- ========================= --%>

<dialog
    id="cancelarReservaModal"
    class="reserva-action-modal"
    data-action-base="${ctx}/admin/reservas"
>

    <form
        method="post"
        id="cancelarReservaForm"
        class="stack-form"
    >

        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

        <input
            type="hidden"
            name="_method"
            value="patch"
        >


        <div class="reserva-action-modal-header">

            <div>

                <p class="eyebrow">
                    Reserva
                </p>

                <h2>
                    Cancelar reserva
                </h2>

            </div>


            <button
                type="button"
                class="btn btn-secondary"
                id="fecharCancelarReservaModal"
            >
                Fechar
            </button>

        </div>


        <div class="field">

            <label for="motivoCancelamento">
                Motivo do cancelamento
            </label>

            <textarea
                id="motivoCancelamento"
                name="motivo"
                maxlength="500"
                rows="4"
                required
                placeholder="Informe o motivo do cancelamento"
            ></textarea>

        </div>


        <div class="reserva-modal-actions">

            <button
                type="button"
                class="btn btn-secondary"
                id="voltarCancelamentoBtn"
            >
                Voltar
            </button>


            <button
                type="submit"
                class="btn btn-primary"
            >
                Confirmar cancelamento
            </button>

        </div>

    </form>

</dialog>


<%-- ========================= --%>
<%-- CALENDARIO ADMIN          --%>
<%-- ========================= --%>

<%@ include file="/WEB-INF/jsp/fragments/calendario-admin.jspf" %>


<%-- ========================= --%>
<%-- SCRIPTS                   --%>
<%-- ========================= --%>

<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>

<script src="${ctx}/js/admin-reservas.js"></script>

</body>

</html>