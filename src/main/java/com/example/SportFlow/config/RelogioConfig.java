package com.example.SportFlow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "Relógio" da aplicação no fuso dos estabelecimentos (padrão: America/Sao_Paulo).
 *
 * As reservas guardam data e hora LOCAIS (ex.: 2026-11-30 às 08:00). Para saber se uma
 * reserva já começou ou já terminou, comparamos com a hora atual NESSE fuso, e não com
 * o fuso do servidor (que pode estar em UTC se o backend for publicado na nuvem).
 *
 * Os services recebem este Clock em vez de chamar LocalDateTime.now() direto.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio(@Value("${sportflow.fuso-horario:America/Sao_Paulo}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
