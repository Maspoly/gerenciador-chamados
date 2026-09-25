package br.com.dunnastecnologia.chamados.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.dunnastecnologia.chamados.domain.model.HistoricoReserva;

@Repository
public interface HistoricoReservaRepository extends JpaRepository<HistoricoReserva, UUID> {

    // Busca todo o histórico de alterações de uma reserva específica ordenado da mais recente para a mais antiga
    List<HistoricoReserva> findByReservaIdOrderByDataAlteracaoDesc(UUID reservaId);
}