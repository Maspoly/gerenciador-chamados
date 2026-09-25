package br.com.dunnastecnologia.chamados.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, UUID> {
    
    // Busca apenas as áreas comuns que estão ativas (para a consulta do Morador)
    List<AreaComum> findByAtivaTrue();
}