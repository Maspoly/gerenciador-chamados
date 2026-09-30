package br.com.dunnastecnologia.chamados.application.UserCase;

import java.util.List;
import java.util.UUID;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;

public interface AreaComumUseCases {

    List<AreaComum> listarTodas(AuthenticatedUser administrador);

    List<AreaComum> listarAtivas();

    AreaComum buscarPorId(UUID id);

    AreaComum salvar(AuthenticatedUser administrador, AreaComum areaComum);

    AreaComum desativar(AuthenticatedUser administrador, UUID id);

    AreaComum reativar(AuthenticatedUser administrador, UUID id);
}