package br.com.dunnastecnologia.chamados.infrastructure.controller.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
@RequestMapping("/morador/reservas")
@PreAuthorize("hasRole('MORADOR')")
public class MoradorReservaApiController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;

    public MoradorReservaApiController(
            ReservaUseCases reservaUseCases,
            WebControllerSupport support
    ) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
    }

    @PostMapping
    @Operation(
            summary = "Solicita uma nova reserva de area comum",
            tags = "13 - Morador Web - Reservas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva solicitada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou regra de negocio violada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
	public String solicitarReserva(
			Authentication authentication,
			@RequestParam UUID areaComumId,
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime dataInicio,
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime dataFim,
			RedirectAttributes redirectAttributes
	) {
		var currentUser = support.authenticatedUser(authentication);

		try {
			reservaUseCases.solicitarReserva(currentUser, areaComumId, dataInicio, dataFim);
			redirectAttributes.addFlashAttribute(
					"successMessage",
					"Solicitacao de reserva registrada."
			);
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}

		return "redirect:/morador/reservas";
	}

    @PatchMapping("/{reservaId}/cancelar")
    @Operation(
            summary = "Cancela uma reserva do morador",
            tags = "13 - Morador Web - Reservas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva cancelada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Reserva nao pode ser cancelada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
    })
    public String cancelarReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);

        reservaUseCases.cancelarReserva(reservaId, motivo, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Reserva cancelada.");

        return "redirect:/morador/reservas";
    }

	@GetMapping("/disponibilidade")
	@ResponseBody
	@Operation(
			summary = "Lista a disponibilidade de uma area comum",
			tags = "13 - Morador Web - Reservas"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Disponibilidade consultada com sucesso."),
			@ApiResponse(responseCode = "400", description = "Parametros da consulta invalidos."),
			@ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
	})
	public List<Map<String, Object>> listarDisponibilidade(
			Authentication authentication,
			@RequestParam UUID areaComumId,
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime inicio,
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime fim
	) {
		var currentUser = support.authenticatedUser(authentication);
		var reservas = reservaUseCases.listarDisponibilidadePorAreaEPeriodo(
				currentUser,
				areaComumId,
				inicio,
				fim
		);

		return reservas.stream()
				.map(reserva -> {
					Map<String, Object> dados = new java.util.LinkedHashMap<>();
					dados.put("dataHoraInicio", reserva.getDataHoraInicio());
					dados.put("dataHoraFim", reserva.getDataHoraFim());
					dados.put("status", reserva.getStatus().name());
					return dados;
				})
				.toList();
	}

	@GetMapping("/minhas/calendario")
	@ResponseBody
	@Operation(
			summary = "Lista as reservas do morador autenticado para o calendario",
			tags = "13 - Morador Web - Reservas"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Reservas do morador consultadas com sucesso."),
			@ApiResponse(responseCode = "403", description = "Acesso negado para o perfil autenticado.")
	})
	public List<Map<String, Object>> listarMinhasReservasCalendario(
			Authentication authentication
	) {
		var currentUser = support.authenticatedUser(authentication);
		var reservas = reservaUseCases.listarMinhasReservas(currentUser);

		return reservas.stream()
				.map(reserva -> {
					Map<String, Object> dados = new java.util.LinkedHashMap<>();
					dados.put("areaComumNome", reserva.getAreaComum().getNome());
					dados.put("dataHoraInicio", reserva.getDataHoraInicio());
					dados.put("dataHoraFim", reserva.getDataHoraFim());
					dados.put("status", reserva.getStatus().name());
					return dados;
				})
				.toList();
	}
}