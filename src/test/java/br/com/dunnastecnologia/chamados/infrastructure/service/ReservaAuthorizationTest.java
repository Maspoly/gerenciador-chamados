package br.com.dunnastecnologia.chamados.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.infrastructure.exception.UnauthorizedOperationException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ColaboradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.HistoricoReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;

class ReservaAuthorizationTest {

    @Mock
    private AdministradorRepository administradorRepository;

    @Mock
    private ColaboradorRepository colaboradorRepository;

    @Mock
    private MoradorRepository moradorRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AreaComumRepository areaComumRepository;

    @Mock
    private HistoricoReservaRepository historicoReservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AuthenticatedUserValidator validator = new AuthenticatedUserValidator(
                administradorRepository,
                colaboradorRepository,
                moradorRepository
        );
        reservaService = new ReservaService(
                reservaRepository,
                areaComumRepository,
                moradorRepository,
                historicoReservaRepository,
                usuarioRepository,
                validator
        );
    }

    @Test
    void moradorAtivoPodeListarSuasReservas() {
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser morador = new AuthenticatedUser(
                moradorId,
                "morador@condominio.local",
                "ROLE_MORADOR"
        );
        when(moradorRepository.existsByIdAndAtivoTrue(moradorId)).thenReturn(true);
        when(reservaRepository.findByMoradorIdOrderByDataCriacaoDesc(moradorId))
                .thenReturn(List.of());

        assertEquals(List.of(), reservaService.listarMinhasReservas(morador));
    }

    @Test
    void administradorAtivoPodeListarTodasAsReservas() {
        UUID administradorId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(
                administradorId,
                "admin@condominio.local",
                "ROLE_ADMINISTRADOR"
        );
        when(administradorRepository.existsByIdAndAtivoTrue(administradorId)).thenReturn(true);
        when(reservaRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of());

        assertEquals(List.of(), reservaService.listarTodas(administrador));
    }

    @Test
    void colaboradorNaoPodeParticiparDoFluxoDeReservas() {
        UUID colaboradorId = UUID.randomUUID();
        AuthenticatedUser colaborador = new AuthenticatedUser(
                colaboradorId,
                "colaborador@condominio.local",
                "ROLE_COLABORADOR"
        );

        UnauthorizedOperationException exception = assertThrows(
                UnauthorizedOperationException.class,
                () -> reservaService.listarMinhasReservas(colaborador)
        );

        assertEquals(
                "Usuario autenticado nao possui perfil de morador",
                exception.getMessage()
        );
    }

    @Test
    void moradorNaoPodeListarTodasAsReservas() {
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser morador = new AuthenticatedUser(
                moradorId,
                "morador@condominio.local",
                "ROLE_MORADOR"
        );
        when(moradorRepository.existsByIdAndAtivoTrue(moradorId)).thenReturn(true);

        assertThrows(
                UnauthorizedOperationException.class,
                () -> reservaService.listarTodas(morador)
        );
    }
}
