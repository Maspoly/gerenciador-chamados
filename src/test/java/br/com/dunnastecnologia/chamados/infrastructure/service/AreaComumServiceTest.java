package br.com.dunnastecnologia.chamados.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;

@ExtendWith(MockitoExtension.class)
class AreaComumServiceTest {

    @Mock
    private AreaComumRepository areaComumRepository;

    @Mock
    private AuthenticatedUserValidator authenticatedUserValidator;

    @InjectMocks
    private AreaComumService areaComumService;

    @Test
    void salvarDevePersistirAreaComumQuandoDadosForemValidos() {
        UUID usuarioId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(usuarioId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        AreaComum area = new AreaComum();
        area.setNome("  Salão de Festas  ");
        area.setDescricao("  Espaço amplo  ");

        doNothing().when(authenticatedUserValidator).assertAdministrador(admin);
        when(areaComumRepository.save(any(AreaComum.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaComum salva = areaComumService.salvar(admin, area);

        assertEquals("Salão de Festas", salva.getNome());
        assertEquals("Espaço amplo", salva.getDescricao());
    }

    @Test
    void salvarDeveRejeitarQuandoNomeForVazio() {
        UUID usuarioId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(usuarioId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        AreaComum area = new AreaComum();
        area.setNome("   ");

        doNothing().when(authenticatedUserValidator).assertAdministrador(admin);

        assertThrows(IllegalArgumentException.class, () -> areaComumService.salvar(admin, area));
    }

    @Test
    void desativarDeveAtualizarAreaComoInativa() {
        UUID usuarioId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(usuarioId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.TRUE);

        doNothing().when(authenticatedUserValidator).assertAdministrador(admin);
        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(areaComumRepository.save(any(AreaComum.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaComum desativada = areaComumService.desativar(admin, areaId);

        assertFalse(desativada.getAtiva());
    }

    @Test
    void reativarDeveAtualizarAreaComoAtiva() {
        UUID usuarioId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(usuarioId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        UUID areaId = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.FALSE);

        doNothing().when(authenticatedUserValidator).assertAdministrador(admin);
        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(areaComumRepository.save(any(AreaComum.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaComum reativada = areaComumService.reativar(admin, areaId);

        assertTrue(reativada.getAtiva());
    }
}
