package com.example.SportFlow.Dto.IA;

import java.util.List;

/** Texto da IA + opções que viram botões "Reservar" no front. */
public record RespostaAssistenteResponse(String mensagem, List<SugestaoReservaResponse> sugestoes) {}
