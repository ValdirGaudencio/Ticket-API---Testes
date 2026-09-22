package com.example.ticktetapi.service;

import com.example.ticktetapi.dto.EventoRequestDTO;
import com.example.ticktetapi.exception.RecursoNaoEncontradoException;
import com.example.ticktetapi.exception.RegraNegocioException;
import com.example.ticktetapi.model.Evento;
import com.example.ticktetapi.repository.EventoRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private EventoService eventoService;

    private EventoRequestDTO dto;

    @BeforeEach
    void setUp() {
        dto = new EventoRequestDTO();
        dto.setNome("Show de Teste");
        dto.setDescricao("Descrição do evento");
        dto.setDataHora(LocalDateTime.now().plusDays(10));
        dto.setLocal("Recife");
        dto.setPreco(new BigDecimal("50.00"));
        dto.setQuantidadeTotal(100);
    }

    @Test
    void deveCriarEventoComSucesso() {
        Evento eventoSalvo = Evento.builder()
                .id(1L)
                .nome("Show de Teste")
                .descricao("Descrição do evento")
                .dataHora(dto.getDataHora())
                .local("Recife")
                .preco(new BigDecimal("50.00"))
                .quantidadeTotal(100)
                .quantidadeDisponivel(100)
                .build();

        when(eventoRepository.save(any(Evento.class))).thenReturn(eventoSalvo);

        Evento resultado = eventoService.criar(dto);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Show de Teste", resultado.getNome());
        assertEquals(100, resultado.getQuantidadeTotal());
        assertEquals(100, resultado.getQuantidadeDisponivel());

        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    void deveListarTodosOsEventos() {
        List<Evento> eventos = List.of(
                Evento.builder().id(1L).nome("Evento 1").build(),
                Evento.builder().id(2L).nome("Evento 2").build()
        );

        when(eventoRepository.findAll()).thenReturn(eventos);

        List<Evento> resultado = eventoService.listarTodos();

        assertEquals(eventos, resultado);
        verify(eventoRepository).findAll();
    }

    @Test
    void deveBuscarEventoPorId() {
        Evento evento = Evento.builder()
                .id(1L)
                .nome("Evento 1")
                .build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        Evento resultado = eventoService.buscarPorId(1L);

        assertEquals(evento, resultado);
        verify(eventoRepository).findById(1L);
    }

    @Test
    void deveLancarExcecaoAoBuscarEventoInexistente() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> eventoService.buscarPorId(1L)
        );

        verify(eventoRepository).findById(1L);
    }

    @Test
    void deveAtualizarEventoComSucesso() {
        Evento evento = Evento.builder()
                .id(1L)
                .nome("Evento Antigo")
                .descricao("Descrição antiga")
                .dataHora(LocalDateTime.now().plusDays(5))
                .local("Local antigo")
                .preco(new BigDecimal("30.00"))
                .quantidadeTotal(100)
                .quantidadeDisponivel(70)
                .build();

        dto.setNome("Evento Atualizado");
        dto.setDescricao("Nova descrição");
        dto.setDataHora(LocalDateTime.now().plusDays(20));
        dto.setLocal("Novo local");
        dto.setPreco(new BigDecimal("60.00"));
        dto.setQuantidadeTotal(150);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(evento)).thenReturn(evento);

        Evento resultado = eventoService.atualizar(1L, dto);

        assertEquals("Evento Atualizado", resultado.getNome());
        assertEquals("Nova descrição", resultado.getDescricao());
        assertEquals("Novo local", resultado.getLocal());
        assertEquals(new BigDecimal("60.00"), resultado.getPreco());
        assertEquals(150, resultado.getQuantidadeTotal());
        assertEquals(120, resultado.getQuantidadeDisponivel());

        verify(eventoRepository).save(evento);
    }

    @Test
    void naoDeveAtualizarQuandoNovaQuantidadeForMenorQueIngressosVendidos() {
        Evento evento = Evento.builder()
                .id(1L)
                .quantidadeTotal(100)
                .quantidadeDisponivel(70)
                .build();

        dto.setQuantidadeTotal(20);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        assertThrows(
                RegraNegocioException.class,
                () -> eventoService.atualizar(1L, dto)
        );

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void deveDeletarEventoComSucesso() {
        Evento evento = Evento.builder()
                .id(1L)
                .nome("Evento para deletar")
                .build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        eventoService.deletar(1L);

        verify(eventoRepository).findById(1L);
        verify(eventoRepository).delete(evento);
    }

    @Test
    void naoDeveDeletarEventoInexistente() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> eventoService.deletar(1L)
        );

        verify(eventoRepository, never()).delete(any());
    }
}