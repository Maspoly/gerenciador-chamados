package br.com.dunnastecnologia.chamados.infrastructure.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;

@Service
@Transactional(readOnly = true)
public class AreaComumService implements AreaComumUseCases {

    private final AreaComumRepository areaComumRepository;
    private final AuthenticatedUserValidator authenticatedUserValidator;

    public AreaComumService(AreaComumRepository areaComumRepository,
                            AuthenticatedUserValidator authenticatedUserValidator) {
        this.areaComumRepository = areaComumRepository;
        this.authenticatedUserValidator = authenticatedUserValidator;
    }

    public List<AreaComum> listarTodas(AuthenticatedUser administrador) {
        authenticatedUserValidator.assertAdministrador(administrador);
        return areaComumRepository.findAll();
    }

    public List<AreaComum> listarAtivas() {
        return areaComumRepository.findByAtivaTrue();
    }

    public AreaComum buscarPorId(UUID id) {
        return areaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área comum não encontrada com o ID: " + id));
    }

    @Override
    @Transactional
    public AreaComum salvar(
            AuthenticatedUser administrador,
            AreaComum areaComum
    ) {
        authenticatedUserValidator.assertAdministrador(administrador);

        if (areaComum.getNome() == null
                || areaComum.getNome().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "O nome da área comum não pode ser vazio."
            );
        }

        String nome = areaComum.getNome().trim();

        if (nome.length() > 255) {
            throw new IllegalArgumentException(
                    "O nome da área comum deve ter no máximo 255 caracteres."
            );
        }

        String descricao = areaComum.getDescricao();

        if (descricao != null) {
            descricao = descricao.trim();

            if (descricao.length() > 500) {
                throw new IllegalArgumentException(
                        "A descrição deve ter no máximo 500 caracteres."
                );
            }

            if (descricao.isEmpty()) {
                descricao = null;
            }
        }

        areaComum.setNome(nome);
        areaComum.setDescricao(descricao);

        return areaComumRepository.save(areaComum);
    }

    @Transactional
    public AreaComum desativar(AuthenticatedUser administrador, UUID id) {
        authenticatedUserValidator.assertAdministrador(administrador);
        AreaComum area = buscarPorId(id);
        area.setAtiva(false);
        return areaComumRepository.save(area);
    }

    @Transactional
    public AreaComum reativar(AuthenticatedUser administrador, UUID id) {
        authenticatedUserValidator.assertAdministrador(administrador);
        AreaComum area = buscarPorId(id);
        area.setAtiva(true);
        return areaComumRepository.save(area);
    }
}