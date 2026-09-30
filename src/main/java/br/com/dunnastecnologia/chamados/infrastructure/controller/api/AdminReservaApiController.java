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
@RequestMapping("/admin/reservas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReservaApiController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;

    public AdminReservaApiController(
            ReservaUseCases reservaUseCases,
            WebControllerSupport support
    ) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
    }

	@PatchMapping("/{reservaId}/aprovar")
	@Operation(
			summary = "Aprova uma reserva",
			tags = "Admin - Reservas"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "302", description = "Reserva aprovada com sucesso."),
			@ApiResponse(responseCode = "400", description = "Reserva nao pode ser aprovada."),
			@ApiResponse(responseCode = "403", description = "Acesso negado.")
	})
	public String aprovarReserva(
			Authentication authentication,
			@PathVariable UUID reservaId,
			RedirectAttributes redirectAttributes
	) {
	var currentUser = support.authenticatedUser(authentication);

	try {

			reservaUseCases.aprovarReserva(
					reservaId,
					currentUser
			);

			redirectAttributes.addFlashAttribute(
					"successMessage",
					"Reserva aprovada."
			);

	} catch (IllegalStateException exception) {

			redirectAttributes.addFlashAttribute(
					"errorMessage",
					exception.getMessage()
			);
	}

	return "redirect:/admin/reservas";
	}

    @PatchMapping("/{reservaId}/negar")
    @Operation(
            summary = "Nega uma reserva",
            tags = "Admin - Reservas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva negada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Reserva nao pode ser negada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado.")
    })
    public String negarReserva(
            Authentication authentication,
            @PathVariable UUID reservaId,
            @RequestParam String motivo,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);

        reservaUseCases.negarReserva(
                reservaId,
                motivo,
                currentUser
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Reserva negada."
        );

        return "redirect:/admin/reservas";
    }

    @PatchMapping("/{reservaId}/cancelar")
    @Operation(
            summary = "Cancela uma reserva",
            tags = "Admin - Reservas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Reserva cancelada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Reserva nao pode ser cancelada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado.")
    })
	public String cancelarReserva(
        Authentication authentication,
        @PathVariable UUID reservaId,
		@RequestParam(required = false) String motivo,
        RedirectAttributes redirectAttributes
	) {
		var currentUser =
				support.authenticatedUser(authentication);

		try {

			reservaUseCases.cancelarReserva(
					reservaId,
					motivo,
					currentUser
			);

			redirectAttributes.addFlashAttribute(
					"successMessage",
					"Reserva cancelada."
			);

		} catch (IllegalStateException | IllegalArgumentException exception) {

			redirectAttributes.addFlashAttribute(
					"errorMessage",
					exception.getMessage()
			);
		}

		return "redirect:/admin/reservas";
	}

	@GetMapping("/calendario")
	@ResponseBody
	@Operation(
			summary = "Lista reservas para o calendario do administrador",
			tags = "Admin - Reservas"
	)
	public List<Map<String, Object>> listarCalendario(
			Authentication authentication,

			@RequestParam UUID areaComumId,

			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime inicio,

			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime fim
	) {
		var currentUser =
				support.authenticatedUser(authentication);

		var reservas =
				reservaUseCases.listarCalendarioAdministrador(
						currentUser,
						areaComumId,
						inicio,
						fim
				);

		return support.mapContent(
				reservas,
				support::toReservaMap
		);
	}
}