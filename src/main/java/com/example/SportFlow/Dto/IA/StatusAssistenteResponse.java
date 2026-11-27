package com.example.SportFlow.Dto.IA;

/** O front pergunta isto para saber se mostra o chat ou o aviso "IA não configurada". */
public record StatusAssistenteResponse(boolean disponivel, String provedor, String modelo) {}
