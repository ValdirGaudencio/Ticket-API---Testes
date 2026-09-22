package com.example.ticktetapi.service;

import com.example.ticktetapi.dto.CompraIngressoRequestDTO;
import com.example.ticktetapi.exception.RecursoNaoEncontradoException;
import com.example.ticktetapi.exception.RegraNegocioException;
import com.example.ticktetapi.model.Cliente;
import com.example.ticktetapi.model.Evento;
import com.example.ticktetapi.model.Ingresso;
import com.example.ticktetapi.model.StatusIngresso;
import com.example.ticktetapi.repository.ClienteRepository;
import com.example.ticktetapi.repository.EventoRepository;
import com.example.ticktetapi.repository.IngressoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngressoServiceTest {

    @Mock
    private IngressoRepository ingressoRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private IngressoService ingressoService;

    private Evento evento;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        evento = Evento.builder()
                .id(1L)
                .nome("Show de Teste")
                .descricao("Evento para testes")
                .dataHora(LocalDateTime.now().plusDays(10))
                .local("Recife")
                .preco(new BigDecimal("50.00"))
                .quantidadeTotal(100)
                .quantidadeDisponivel(100)
                .build();

        cliente = Cliente.builder()
                .id(1L)
                .nome("João")
                .email("joao@email.com")
                .cpf("12345678900")
                .build();
    }

    @Test
    void deveComprarIngressoComSucesso() {
        CompraIngressoRequestDTO dto = new CompraIngressoRequestDTO();
        dto.setEventoId(1L);
        dto.setClienteId(1L);
        dto.setQuantidade(2);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(ingressoRepository.save(any(Ingresso.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Ingresso> resultado = ingressoService.comprar(dto);

        assertEquals(2, resultado.size());
        assertEquals(98, evento.getQuantidadeDisponivel());

        verify(ingressoRepository, times(2)).save(any(Ingresso.class));
        verify(eventoRepository).save(evento);

        assertEquals(StatusIngresso.ATIVO, resultado.get(0).getStatus());
        assertEquals(evento, resultado.get(0).getEvento());
        assertEquals(cliente, resultado.get(0).getCliente());
        assertEquals(new BigDecimal("50.00"), resultado.get(0).getValorPago());
    }

    @Test
    void deveLancarExcecaoQuandoEventoNaoExiste() {
        CompraIngressoRequestDTO dto = new CompraIngressoRequestDTO();
        dto.setEventoId(1L);
        dto.setClienteId(1L);
        dto.setQuantidade(1);

        when(eventoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> ingressoService.comprar(dto)
        );

        verify(clienteRepository, never()).findById(anyLong());
        verify(ingressoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoExiste() {
        CompraIngressoRequestDTO dto = new CompraIngressoRequestDTO();
        dto.setEventoId(1L);
        dto.setClienteId(1L);
        dto.setQuantidade(1);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> ingressoService.comprar(dto)
        );

        verify(ingressoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoEventoJaOcorreu() {
        evento.setDataHora(LocalDateTime.now().minusDays(1));

        CompraIngressoRequestDTO dto = new CompraIngressoRequestDTO();
        dto.setEventoId(1L);
        dto.setClienteId(1L);
        dto.setQuantidade(1);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(
                RegraNegocioException.class,
                () -> ingressoService.comprar(dto)
        );

        verify(ingressoRepository, never()).save(any());
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaIngressosSuficientes() {
        evento.setQuantidadeDisponivel(1);

        CompraIngressoRequestDTO dto = new CompraIngressoRequestDTO();
        dto.setEventoId(1L);
        dto.setClienteId(1L);
        dto.setQuantidade(2);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(
                RegraNegocioException.class,
                () -> ingressoService.comprar(dto)
        );

        verify(ingressoRepository, never()).save(any());
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void deveListarTodosOsIngressos() {
        List<Ingresso> ingressos = List.of(
                Ingresso.builder().id(1L).build(),
                Ingresso.builder().id(2L).build()
        );

        when(ingressoRepository.findAll()).thenReturn(ingressos);

        List<Ingresso> resultado = ingressoService.listarTodos();

        assertEquals(ingressos, resultado);
        verify(ingressoRepository).findAll();
    }

    @Test
    void deveBuscarIngressoPorId() {
        Ingresso ingresso = Ingresso.builder()
                .id(1L)
                .evento(evento)
                .cliente(cliente)
                .status(StatusIngresso.ATIVO)
                .build();

        when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));

        Ingresso resultado = ingressoService.buscarPorId(1L);

        assertEquals(ingresso, resultado);
        verify(ingressoRepository).findById(1L);
    }

    @Test
    void deveLancarExcecaoAoBuscarIngressoInexistente() {
        when(ingressoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> ingressoService.buscarPorId(1L)
        );
    }

    @Test
    void deveListarIngressosPorEvento() {
        List<Ingresso> ingressos = List.of(
                Ingresso.builder().id(1L).evento(evento).build()
        );

        when(ingressoRepository.findByEventoId(1L)).thenReturn(ingressos);

        List<Ingresso> resultado = ingressoService.listarPorEvento(1L);

        assertEquals(ingressos, resultado);
        verify(ingressoRepository).findByEventoId(1L);
    }

    @Test
    void deveListarIngressosPorCliente() {
        List<Ingresso> ingressos = List.of(
                Ingresso.builder().id(1L).cliente(cliente).build()
        );

        when(ingressoRepository.findByClienteId(1L)).thenReturn(ingressos);

        List<Ingresso> resultado = ingressoService.listarPorCliente(1L);

        assertEquals(ingressos, resultado);
        verify(ingressoRepository).findByClienteId(1L);
    }

    @Test
    void deveCancelarIngressoComSucesso() {
        Ingresso ingresso = Ingresso.builder()
                .id(1L)
                .evento(evento)
                .cliente(cliente)
                .status(StatusIngresso.ATIVO)
                .build();

        evento.setQuantidadeDisponivel(50);

        when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));
        when(ingressoRepository.save(ingresso)).thenReturn(ingresso);
        when(eventoRepository.save(evento)).thenReturn(evento);

        Ingresso resultado = ingressoService.cancelar(1L);

        assertEquals(StatusIngresso.CANCELADO, resultado.getStatus());
        assertEquals(51, evento.getQuantidadeDisponivel());

        verify(ingressoRepository).save(ingresso);
        verify(eventoRepository).save(evento);
    }

    @Test
    void naoDeveCancelarIngressoJaCancelado() {
        Ingresso ingresso = Ingresso.builder()
                .id(1L)
                .evento(evento)
                .status(StatusIngresso.CANCELADO)
                .build();

        when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));

        assertThrows(
                RegraNegocioException.class,
                () -> ingressoService.cancelar(1L)
        );

        verify(ingressoRepository, never()).save(any());
    }

    @Test
    void naoDeveCancelarIngressoUtilizado() {
        Ingresso ingresso = Ingresso.builder()
                .id(1L)
                .evento(evento)
                .status(StatusIngresso.UTILIZADO)
                .build();

        when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));

        assertThrows(
                RegraNegocioException.class,
                () -> ingressoService.cancelar(1L)
        );

        verify(ingressoRepository, never()).save(any());
    }

    @Test
    void deveMarcarIngressoComoUtilizado() {
        Ingresso ingresso = Ingresso.builder()
                .id(1L)
                .evento(evento)
                .cliente(cliente)
                .status(StatusIngresso.ATIVO)
                .build();

        when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));
        when(ingressoRepository.save(ingresso)).thenReturn(ingresso);

        Ingresso resultado = ingressoService.marcarComoUtilizado(1L);

        assertEquals(StatusIngresso.UTILIZADO, resultado.getStatus());
        verify(ingressoRepository).save(ingresso);
    }

    @Test
    void naoDeveMarcarIngressoCanceladoComoUtilizado() {
        Ingresso ingresso = Ingresso.builder()
                .id(1L)
                .status(StatusIngresso.CANCELADO)
                .build();

        when(ingressoRepository.findById(1L)).thenReturn(Optional.of(ingresso));

        assertThrows(
                RegraNegocioException.class,
                () -> ingressoService.marcarComoUtilizado(1L)
        );

        verify(ingressoRepository, never()).save(any());
    }
}
