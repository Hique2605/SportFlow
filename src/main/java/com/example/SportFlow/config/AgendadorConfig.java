package com.example.SportFlow.config;

import com.example.SportFlow.Service.AgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Tarefa agendada que encerra as reservas cujo horário já passou:
 *   CONFIRMADO -> CONCLUIDO  (o jogo aconteceu)
 *   PENDENTE   -> CANCELADO  (ninguém confirmou até o fim do horário)
 *
 * Roda 30 segundos depois de a aplicação subir e depois a cada 5 minutos
 * (configurável em application.properties). Nos testes fica desligada
 * (sportflow.agendador.ativo=false) e o método é chamado direto.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sportflow.agendador.ativo", havingValue = "true", matchIfMissing = true)
public class AgendadorConfig {

    private final AgendamentoService agendamentoService;

    @Scheduled(initialDelayString = "${sportflow.agendador.atraso-inicial-ms:30000}",
               fixedDelayString = "${sportflow.agendador.intervalo-ms:300000}")
    public void encerrarReservasPassadas() {
        agendamentoService.encerrarAgendamentosPassados();
    }
}
