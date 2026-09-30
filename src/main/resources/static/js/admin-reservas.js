document.addEventListener("DOMContentLoaded", () => {
    // ========================================
    // MODAL - NEGAR RESERVA
    // ========================================

    const negarModal = document.getElementById("negarReservaModal");

    const negarForm = document.getElementById("negarReservaForm");

    const motivoNegativa = document.getElementById("motivoNegativa");

    document.querySelectorAll(".js-negar-reserva").forEach(botao => {
        botao.addEventListener("click", () => {
            if (!negarModal || !negarForm || !motivoNegativa) {
                return;
            }

            const reservaId = botao.dataset.reservaId;

            const baseUrl = negarModal.dataset.actionBase;

            negarForm.action = `${baseUrl}/${reservaId}/negar`;

            motivoNegativa.value = "";

            negarModal.showModal();

            motivoNegativa.focus();
        });
    });

    document.getElementById("fecharNegarReservaModal")?.addEventListener("click", () => {
        negarModal?.close();
    });

    document.getElementById("cancelarNegativaBtn")?.addEventListener("click", () => {
        negarModal?.close();
    });

    // ========================================
    // MODAL - CANCELAR RESERVA
    // ========================================

    const cancelarModal = document.getElementById("cancelarReservaModal");

    const cancelarForm = document.getElementById("cancelarReservaForm");

    const motivoCancelamento = document.getElementById("motivoCancelamento");

    document.querySelectorAll(".js-cancelar-reserva").forEach(botao => {
        botao.addEventListener("click", () => {
            if (!cancelarModal || !cancelarForm || !motivoCancelamento) {
                return;
            }

            const reservaId = botao.dataset.reservaId;

            const baseUrl = cancelarModal.dataset.actionBase;

            cancelarForm.action = `${baseUrl}/${reservaId}/cancelar`;

            motivoCancelamento.value = "";

            cancelarModal.showModal();

            motivoCancelamento.focus();
        });
    });

    document.getElementById("fecharCancelarReservaModal")?.addEventListener("click", () => {
        cancelarModal?.close();
    });

    document.getElementById("voltarCancelamentoBtn")?.addEventListener("click", () => {
        cancelarModal?.close();
    });

    // ========================================
    // CALENDARIO ADMIN
    // ========================================

    const areaSelect = document.getElementById("adminAreaComumId");

    const abrirCalendarioBtn = document.getElementById("abrirCalendarioAdminBtn");

    const calendarioModal = document.getElementById("calendarioAdminModal");

    const calendarioTitulo = document.getElementById("calendarioAdminTitulo");

    const mesAnoTitulo = document.getElementById("mesAnoAdminTitulo");

    const diasCalendario = document.getElementById("diasCalendarioAdmin");

    const dataSelecionadaTitulo = document.getElementById("dataSelecionadaAdminTitulo");

    const horariosDiaConteudo = document.getElementById("horariosDiaAdminConteudo");

    const fecharCalendarioBtn = document.getElementById("fecharCalendarioAdminBtn");

    const mesAnteriorBtn = document.getElementById("mesAnteriorAdminBtn");

    const proximoMesBtn = document.getElementById("proximoMesAdminBtn");

    /*
     * Se a pagina nao possuir o calendario,
     * nao precisamos inicializar esta parte.
     */

    if (
        !areaSelect ||
        !abrirCalendarioBtn ||
        !calendarioModal ||
        !calendarioTitulo ||
        !mesAnoTitulo ||
        !diasCalendario ||
        !dataSelecionadaTitulo ||
        !horariosDiaConteudo
    ) {
        return;
    }

    let dataAtualCalendario = new Date();

    let reservasCalendario = [];

    // ========================================
    // ABRIR CALENDARIO
    // ========================================

    abrirCalendarioBtn.addEventListener("click", async () => {
        const areaId = areaSelect.value;

        if (!areaId) {
            alert("Selecione uma area comum.");

            return;
        }

        const areaNome = areaSelect.options[areaSelect.selectedIndex].text.trim();

        calendarioTitulo.textContent = `Calendario - ${areaNome}`;

        dataAtualCalendario = new Date(new Date().getFullYear(), new Date().getMonth(), 1);

        /*
         * Abre primeiro o modal para que
         * o usuario perceba imediatamente
         * a acao.
         */

        calendarioModal.showModal();

        await atualizarCalendario();
    });

    // ========================================
    // FECHAR CALENDARIO
    // ========================================

    fecharCalendarioBtn?.addEventListener("click", () => {
        calendarioModal.close();
    });

    // ========================================
    // MES ANTERIOR
    // ========================================

    mesAnteriorBtn?.addEventListener("click", async () => {
        dataAtualCalendario = new Date(
            dataAtualCalendario.getFullYear(),

            dataAtualCalendario.getMonth() - 1,

            1,
        );

        await atualizarCalendario();
    });

    // ========================================
    // PROXIMO MES
    // ========================================

    proximoMesBtn?.addEventListener("click", async () => {
        dataAtualCalendario = new Date(
            dataAtualCalendario.getFullYear(),

            dataAtualCalendario.getMonth() + 1,

            1,
        );

        await atualizarCalendario();
    });

    // ========================================
    // ATUALIZAR CALENDARIO
    // ========================================

    async function atualizarCalendario() {
        try {
            horariosDiaConteudo.innerHTML = "<p>Carregando reservas...</p>";

            await carregarReservasCalendario();

            renderizarCalendario();
        } catch (erro) {
            reservasCalendario = [];

            diasCalendario.innerHTML = "";

            dataSelecionadaTitulo.textContent = "Nao foi possivel carregar o calendario";

            horariosDiaConteudo.textContent = "Nao foi possivel carregar as reservas.";
        }
    }

    // ========================================
    // BUSCAR RESERVAS NO BACKEND
    // ========================================

    async function carregarReservasCalendario() {
        const areaId = areaSelect.value;

        if (!areaId) {
            reservasCalendario = [];

            return;
        }

        const ano = dataAtualCalendario.getFullYear();

        const mes = dataAtualCalendario.getMonth();

        /*
         * Primeiro instante do mes atual.
         */

        const inicio = new Date(ano, mes, 1, 0, 0, 0);

        /*
         * Primeiro instante do proximo mes.
         *
         * Dessa forma usamos intervalo:
         *
         * [inicio, fim)
         */

        const fim = new Date(ano, mes + 1, 1, 0, 0, 0);

        /*
         * O modal de cancelamento ja possui:
         *
         * data-action-base="${ctx}/admin/reservas"
         *
         * Reaproveitamos esse valor para
         * respeitar o context path da aplicacao.
         */

        const baseUrl = cancelarModal?.dataset.actionBase ?? negarModal?.dataset.actionBase;

        if (!baseUrl) {
            throw new Error("URL base das reservas nao encontrada.");
        }

        const parametros = new URLSearchParams({
            areaComumId: areaId,

            inicio: formatarDataBackend(inicio),

            fim: formatarDataBackend(fim),
        });

        const response = await fetch(`${baseUrl}/calendario?${parametros.toString()}`);

        if (!response.ok) {
            throw new Error(`Erro HTTP ${response.status}`);
        }

        reservasCalendario = await response.json();
    }

    // ========================================
    // RENDERIZAR CALENDARIO
    // ========================================

    function renderizarCalendario() {
        diasCalendario.innerHTML = "";

        horariosDiaConteudo.innerHTML = "";

        dataSelecionadaTitulo.textContent = "Selecione um dia";

        const ano = dataAtualCalendario.getFullYear();

        const mes = dataAtualCalendario.getMonth();

        mesAnoTitulo.textContent = new Intl.DateTimeFormat("pt-BR", {
            month: "long",
            year: "numeric",
        }).format(new Date(ano, mes, 1));

        const primeiroDiaSemana = new Date(ano, mes, 1).getDay();

        const totalDias = new Date(ano, mes + 1, 0).getDate();

        // ========================================
        // ESPACOS ANTES DO PRIMEIRO DIA
        // ========================================

        for (let i = 0; i < primeiroDiaSemana; i++) {
            const vazio = document.createElement("div");

            vazio.className = "reserva-calendar-day empty";

            diasCalendario.appendChild(vazio);
        }

        // ========================================
        // DIAS DO MES
        // ========================================

        for (let dia = 1; dia <= totalDias; dia++) {
            const botaoDia = document.createElement("button");

            botaoDia.type = "button";

            botaoDia.className = "reserva-calendar-day";

            botaoDia.textContent = dia;

            const reservasDoDia = buscarReservasDoDia(ano, mes, dia);

            const possuiAprovada = reservasDoDia.some(reserva => reserva.status === "APROVADA");

            const possuiSolicitada = reservasDoDia.some(reserva => reserva.status === "SOLICITADA");

            const possuiNegada = reservasDoDia.some(reserva => reserva.status === "NEGADA");

            const possuiCancelada = reservasDoDia.some(reserva => reserva.status === "CANCELADA");

            /*
             * PRIORIDADE VISUAL
             *
             * APROVADA
             * -> existe ocupacao real.
             *
             * SOLICITADA
             * -> existe pedido pendente.
             *
             * NEGADA / CANCELADA
             * -> apenas historico.
             *
             * Se houver APROVADA junto com
             * qualquer outro estado, a cor
             * de APROVADA prevalece.
             */

            if (possuiAprovada) {
                botaoDia.classList.add("has-reservation");
            } else if (possuiSolicitada) {
                botaoDia.classList.add("has-request");
            } else if (possuiNegada && possuiCancelada) {
                botaoDia.classList.add("has-history");
            } else if (possuiNegada) {
                botaoDia.classList.add("has-denied");
            } else if (possuiCancelada) {
                botaoDia.classList.add("has-canceled");
            }

            /*
             * Tooltip simples.
             */

            if (reservasDoDia.length === 1) {
                botaoDia.title = "1 reserva neste dia";
            } else if (reservasDoDia.length > 1) {
                botaoDia.title = `${reservasDoDia.length} reservas neste dia`;
            }

            botaoDia.addEventListener("click", () => {
                mostrarReservasDoDia(ano, mes, dia);
            });

            diasCalendario.appendChild(botaoDia);
        }
    }

    // ========================================
    // RESERVAS QUE PASSAM PELO DIA
    // ========================================

    function buscarReservasDoDia(ano, mes, dia) {
        const inicioDia = new Date(ano, mes, dia, 0, 0, 0, 0);

        const fimDia = new Date(ano, mes, dia + 1, 0, 0, 0, 0);

        return reservasCalendario.filter(reserva => {
            const inicioReserva = new Date(reserva.dataHoraInicio);

            const fimReserva = new Date(reserva.dataHoraFim);

            /*
             * Sobreposicao:
             *
             * inicioReserva < fimDia
             * &&
             * fimReserva > inicioDia
             *
             * Isso tambem trata reservas
             * que atravessam a meia-noite.
             */

            return inicioReserva < fimDia && fimReserva > inicioDia;
        });
    }

    // ========================================
    // MOSTRAR RESERVAS DE UM DIA
    // ========================================

    function mostrarReservasDoDia(ano, mes, dia) {
        const data = new Date(ano, mes, dia);

        dataSelecionadaTitulo.textContent = new Intl.DateTimeFormat("pt-BR", {
            dateStyle: "full",
        }).format(data);

        horariosDiaConteudo.innerHTML = "";

        const reservasDoDia = buscarReservasDoDia(ano, mes, dia).sort(
            (a, b) => new Date(a.dataHoraInicio) - new Date(b.dataHoraInicio),
        );

        if (reservasDoDia.length === 0) {
            const mensagem = document.createElement("p");

            mensagem.textContent = "Nenhuma reserva para este dia.";

            horariosDiaConteudo.appendChild(mensagem);

            return;
        }

        reservasDoDia.forEach(reserva => {
            const item = document.createElement("div");

            item.className = "reserva-calendar-item";

            // ========================================
            // DATA / HORARIO
            // ========================================

            const horario = document.createElement("strong");

            horario.textContent = `${formatarDataHora(reserva.dataHoraInicio)} - ${formatarDataHora(reserva.dataHoraFim)}`;

            // ========================================
            // MORADOR
            // ========================================

            const morador = document.createElement("p");

            morador.textContent = reserva.moradorNome ? `Morador: ${reserva.moradorNome}` : "Morador nao informado";

            // ========================================
            // STATUS
            // ========================================

            const status = document.createElement("span");

            status.classList.add("reserva-calendar-status", `status-${reserva.status.toLowerCase()}`);

            status.textContent = reserva.status;

            // ========================================
            // MONTAGEM
            // ========================================

            item.appendChild(horario);

            item.appendChild(morador);

            item.appendChild(status);

            horariosDiaConteudo.appendChild(item);
        });
    }

    // ========================================
    // FORMATAR DATA/HORA PARA EXIBICAO
    // ========================================

    function formatarDataHora(valor) {
        const data = new Date(valor);

        return data.toLocaleString("pt-BR", {
            day: "2-digit",
            month: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    // ========================================
    // FORMATAR DATA PARA O BACKEND
    // ========================================

    function formatarDataBackend(data) {
        const ano = data.getFullYear();

        const mes = String(data.getMonth() + 1).padStart(2, "0");

        const dia = String(data.getDate()).padStart(2, "0");

        const hora = String(data.getHours()).padStart(2, "0");

        const minuto = String(data.getMinutes()).padStart(2, "0");

        const segundo = String(data.getSeconds()).padStart(2, "0");

        return `${ano}-${mes}-${dia}` + `T${hora}:${minuto}:${segundo}`;
    }
});
