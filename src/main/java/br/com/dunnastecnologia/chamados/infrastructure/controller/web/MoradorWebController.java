package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.dunnastecnologia.chamados.application.UserCase.AnexoChamadoUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ComentarioUseCase;
import br.com.dunnastecnologia.chamados.application.UserCase.MoradorUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.StatusChamadoUseCase;
import br.com.dunnastecnologia.chamados.application.UserCase.TipoChamadoUseCase;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.AbrirChamadoForm;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.ComentarioForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
@RequestMapping("/morador")
@PreAuthorize("hasRole('MORADOR')")
public class MoradorWebController {

private final MoradorUseCases moradorUseCases;

        private final TipoChamadoUseCase tipoChamadoUseCase;
        private final StatusChamadoUseCase statusChamadoUseCase;
        private final AnexoChamadoUseCases anexoChamadoUseCases;
        private final ComentarioUseCase comentarioUseCase;
        private final ReservaUseCases reservaUseCases;
        private final WebControllerSupport support;
        private final AreaComumUseCases areaComumUseCases;

        public MoradorWebController(
                MoradorUseCases moradorUseCases,
                TipoChamadoUseCase tipoChamadoUseCase,
                StatusChamadoUseCase statusChamadoUseCase,
                AnexoChamadoUseCases anexoChamadoUseCases,
                ComentarioUseCase comentarioUseCase,
                ReservaUseCases reservaUseCases,
                AreaComumUseCases areaComumUseCases,
                WebControllerSupport support
        ) {
        this.moradorUseCases = moradorUseCases;
        this.tipoChamadoUseCase = tipoChamadoUseCase;
        this.statusChamadoUseCase = statusChamadoUseCase;
        this.anexoChamadoUseCases = anexoChamadoUseCases;
        this.comentarioUseCase = comentarioUseCase;
        this.reservaUseCases = reservaUseCases;
        this.areaComumUseCases = areaComumUseCases;
        this.support = support;
        }

    @ModelAttribute("abrirChamadoForm")
    public AbrirChamadoForm abrirChamadoForm() {
        return new AbrirChamadoForm();
    }

    @ModelAttribute("comentarioForm")
    public ComentarioForm comentarioForm() {
        return new ComentarioForm();
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Exibe o dashboard do morador", tags = "12 - Morador Web - Paginas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina inicial do morador renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    public String dashboard(
            Authentication authentication,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var unidades = moradorUseCases.listarMinhasUnidades(currentUser, support.pageRequest(0, 20));
        var chamados = moradorUseCases.listarMeusChamados(currentUser, null, null, null, null, support.pageRequest(0, 5));

        model.addAttribute("pageTitle", "Meu Painel");
        model.addAttribute("minhasUnidades", support.mapContent(unidades.content(), support::toUnidadeMap));
        model.addAttribute("meusChamados", support.mapContent(chamados.content(), support::toChamadoMap));
        model.addAttribute("totalUnidades", unidades.totalElements());
        model.addAttribute("totalChamados", chamados.totalElements());
        return "morador/dashboard";
    }

    @GetMapping("/chamados")
    @Transactional(readOnly = true)
    @Operation(summary = "Lista os chamados do morador com filtros e paginacao", tags = "12 - Morador Web - Paginas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de chamados do morador renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    public String listarChamados(
            Authentication authentication,
            @RequestParam(required = false) UUID statusId,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID tipoChamadoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAbertura,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var chamados = moradorUseCases.listarMeusChamados(
                currentUser,
                statusId,
                unidadeId,
                tipoChamadoId,
                dataAbertura,
                support.pageRequest(page, size)
        );
        var status = statusChamadoUseCase.listarStatus(support.pageRequest(0, 100));
        var unidades = moradorUseCases.listarMinhasUnidades(currentUser, support.pageRequest(0, 100));
        var tipos = tipoChamadoUseCase.listarTiposChamado(support.pageRequest(0, 100));

        model.addAttribute("pageTitle", "Meus Chamados");
        model.addAttribute("chamados", support.mapContent(chamados.content(), support::toChamadoMap));
        model.addAttribute("chamadosPage", support.pageMetadata(chamados));
        model.addAttribute("statusDisponiveis", support.mapContent(status.content(), support::toStatusChamadoMap));
        model.addAttribute("unidadesDisponiveis", support.mapContent(unidades.content(), support::toUnidadeMap));
        model.addAttribute("tiposChamadoDisponiveis", support.mapContent(tipos.content(), support::toTipoChamadoMap));
        model.addAttribute("filtroStatusId", statusId);
        model.addAttribute("filtroUnidadeId", unidadeId);
        model.addAttribute("filtroTipoChamadoId", tipoChamadoId);
        model.addAttribute("filtroDataAbertura", dataAbertura);
        return "morador/chamados/lista";
    }

    @GetMapping("/chamados/novo")
    @Transactional(readOnly = true)
    @Operation(summary = "Exibe o formulario para abertura de chamado", tags = "12 - Morador Web - Paginas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formulario de abertura de chamado renderizado com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    public String novoChamado(
            Authentication authentication,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var unidades = moradorUseCases.listarMinhasUnidades(currentUser, support.pageRequest(0, 100));
        var tipos = tipoChamadoUseCase.listarTiposChamado(support.pageRequest(0, 100));

        model.addAttribute("pageTitle", "Abrir Chamado");
        model.addAttribute("unidades", support.mapContent(unidades.content(), support::toUnidadeMap));
        model.addAttribute("tiposChamado", support.mapContent(tipos.content(), support::toTipoChamadoMap));
        return "morador/chamados/novo";
    }

    @GetMapping("/chamados/{chamadoId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Exibe o detalhe de um chamado do morador", tags = "12 - Morador Web - Paginas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagina de detalhe do chamado renderizada com sucesso."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado."),
            @ApiResponse(responseCode = "404", description = "Chamado nao encontrado para o morador.")
    })
    public String detalharChamado(
            Authentication authentication,
            @PathVariable UUID chamadoId,
            Model model
    ) {
        var currentUser = support.authenticatedUser(authentication);
        var chamado = moradorUseCases.buscarMeuChamadoPorId(currentUser, chamadoId);
        var comentarios = comentarioUseCase.listarComentariosDoChamado(currentUser, chamadoId, support.pageRequest(0, 100));
        var anexos = anexoChamadoUseCases.listarAnexosDoChamado(currentUser, chamadoId, support.pageRequest(0, 100));

        model.addAttribute("pageTitle", "Detalhes do Chamado");
        model.addAttribute("chamado", support.toChamadoMap(chamado));
        model.addAttribute("comentarios", support.mapContent(comentarios.content(), support::toComentarioMap));
        model.addAttribute(
                "anexos",
                anexos.content().stream()
                        .map(anexo -> support.toAnexoMap(
                                anexo.id(),
                                anexo.nomeArquivo(),
                                anexo.contentType(),
                                anexo.tamanhoBytes()
                        ))
                        .toList()
        );
        return "morador/chamados/detalhe";
    }

	@GetMapping("/reservas")
	@Transactional(readOnly = true)
	@Operation(
			summary = "Lista as reservas do morador autenticado",
			tags = "12 - Morador Web - Paginas"
	)
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Pagina de reservas do morador renderizada com sucesso."
			),
			@ApiResponse(
					responseCode = "403",
					description = "Acesso negado para o perfil autenticado."
			)
	})
	public String listarReservas(
			Authentication authentication,

			@RequestParam(required = false)
			UUID areaComumId,

			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate dataInicio,

			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate dataFim,

			Model model
	) {
	var currentUser = support.authenticatedUser(authentication);

	var minhasReservas =
			reservaUseCases.listarMinhasReservas(currentUser);

	var areasAtivas =
			areaComumUseCases.listarAtivas();

	LocalDate inicioPeriodo = dataInicio != null
			? dataInicio
			: LocalDate.now().withDayOfMonth(1);

	LocalDate fimPeriodo = dataFim != null
			? dataFim
			: inicioPeriodo.plusMonths(1).minusDays(1);

	if (fimPeriodo.isBefore(inicioPeriodo)) {
			throw new IllegalArgumentException(
					"A data final não pode ser anterior à data inicial."
			);
	}

	UUID areaSelecionadaId = areaComumId;

	if (areaSelecionadaId == null && !areasAtivas.isEmpty()) {
			areaSelecionadaId = areasAtivas.get(0).getId();
	}

	List<Reserva> reservasAprovadas = List.of();

	if (areaSelecionadaId != null) {

			final UUID areaId = areaSelecionadaId;

			boolean areaAtiva = areasAtivas.stream()
					.anyMatch(area -> area.getId().equals(areaId));

			if (!areaAtiva) {
			throw new IllegalArgumentException(
					"Área comum inválida ou desativada."
			);
			}

			reservasAprovadas =
					reservaUseCases.listarAprovadasPorAreaEPeriodo(
							currentUser,
							areaSelecionadaId,
							inicioPeriodo.atStartOfDay(),
							fimPeriodo.plusDays(1).atStartOfDay()
					);
	}

        var reservasMap = minhasReservas.stream()
        .map(reserva -> {

            var values =
                    support.toReservaMap(reserva);

            if (reserva.getStatus() == StatusReserva.CANCELADA) {

                String motivoCancelamento =
                        reservaUseCases.buscarMotivoCancelamento(
                                currentUser,
                                reserva.getId()
                        );

                values.put(
                        "motivoCancelamento",
                        motivoCancelamento
                );
            }

            return values;
        })
        .toList();


	model.addAttribute("pageTitle", "Minhas Reservas");

        model.addAttribute(
                "reservas",
                reservasMap
        );

	model.addAttribute(
			"areasAtivas",
			support.mapContent(
					areasAtivas,
					support::toAreaComumMap
			)
	);

	model.addAttribute(
			"reservasAprovadas",
			support.mapContent(
					reservasAprovadas,
					support::toReservaMap
			)
	);

	model.addAttribute(
			"areaSelecionadaId",
			areaSelecionadaId
	);

	model.addAttribute(
			"dataInicio",
			inicioPeriodo
	);

	model.addAttribute(
			"dataFim",
			fimPeriodo
	);

	return "morador/reservas/lista";
	}
}
