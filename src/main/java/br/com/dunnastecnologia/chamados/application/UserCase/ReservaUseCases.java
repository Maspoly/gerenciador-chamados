package br.com.dunnastecnologia.chamados.application.UserCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;

public interface ReservaUseCases {

    Reserva solicitarReserva(
            AuthenticatedUser morador,
            UUID areaComumId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    );

    Reserva aprovarReserva(
            UUID reservaId,
            AuthenticatedUser administrador
    );

    Reserva negarReserva(
            UUID reservaId,
            String motivo,
            AuthenticatedUser administrador
    );

	Reserva cancelarReserva(
        UUID reservaId,
        String motivo,
        AuthenticatedUser usuarioLogado
	);

    List<Reserva> listarMinhasReservas(
            AuthenticatedUser morador
    );

    List<Reserva> listarTodas(
            AuthenticatedUser administrador
    );

    List<Reserva> listarAprovadasPorAreaEPeriodo(
        AuthenticatedUser morador,
        UUID areaComumId,
        LocalDateTime inicio,
        LocalDateTime fim
	);
	List<Reserva> listarPorStatus(
			AuthenticatedUser administrador,
			StatusReserva status
	);
	List<Reserva> listarCalendarioAdministrador(
        AuthenticatedUser administrador,
        UUID areaComumId,
        LocalDateTime inicio,
        LocalDateTime fim
	);

	String buscarMotivoCancelamento(
        AuthenticatedUser morador,
        UUID reservaId
    );

	List<Reserva> listarDisponibilidadePorAreaEPeriodo(
        AuthenticatedUser morador,
        UUID areaComumId,
        LocalDateTime inicio,
        LocalDateTime fim
	);
}