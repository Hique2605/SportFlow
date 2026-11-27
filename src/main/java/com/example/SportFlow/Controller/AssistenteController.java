package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.IA.MensagemAssistenteRequest;
import com.example.SportFlow.Dto.IA.RespostaAssistenteResponse;
import com.example.SportFlow.Dto.IA.StatusAssistenteResponse;
import com.example.SportFlow.Service.AssistenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Assistente IA", description = "Assistente de reservas em linguagem natural (LangChain4j + Google Gemini). Exige login.")
@RestController
@RequestMapping("/api/ia/assistente")
@RequiredArgsConstructor
public class AssistenteController {

    private final AssistenteService service;

    @Operation(summary = "A IA está configurada? (para o front mostrar ou esconder o chat)")
    @GetMapping("/status")
    public StatusAssistenteResponse status() {
        return service.status();
    }

    @Operation(summary = "Envia uma mensagem ao assistente",
            description = "Ex.: \"quero jogar futebol sábado à noite até R$ 150\". Devolve o texto da IA e até 3 horários livres "
                    + "(sugestoes) com horarioId e dataAgendamento prontos para o POST /agendamentos. "
                    + "503 se a IA não estiver configurada; 429 se passar de 10 mensagens por minuto.")
    @PostMapping
    public RespostaAssistenteResponse conversar(@Valid @RequestBody MensagemAssistenteRequest dto) {
        return service.conversar(dto.mensagem());
    }

    @Operation(summary = "Começa uma nova conversa (apaga o histórico do usuário logado)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void novaConversa() {
        service.novaConversa();
    }
}
