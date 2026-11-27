package com.example.SportFlow.Controller;

import com.example.SportFlow.Dto.Estabelecimento.EstabelecimentoResponse;
import com.example.SportFlow.Dto.User.UsuarioRequest;
import com.example.SportFlow.Dto.User.UsuarioResponse;
import com.example.SportFlow.Dto.User.UsuarioUpdateRequest;
import com.example.SportFlow.Service.EstabelecimentoService;
import com.example.SportFlow.Service.UsuarioService;
import com.example.SportFlow.config.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Usuários", description = "Gestão de usuários (ADMIN). GET /usuarios/me para qualquer usuário logado.")
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;
    private final EstabelecimentoService estabelecimentoService;
    private final UsuarioLogado usuarioLogado;

    @Operation(summary = "Dados do usuário dono do token")
    @GetMapping("/me")
    public UsuarioResponse eu() {
        return UsuarioResponse.from(service.buscar(usuarioLogado.id()));
    }

    @Operation(summary = "Estabelecimentos que o usuário logado gerencia (é funcionário ativo)")
    @GetMapping("/me/estabelecimentos")
    public List<EstabelecimentoResponse> meusEstabelecimentos() {
        return estabelecimentoService.listarGerenciados().stream().map(EstabelecimentoResponse::from).toList();
    }

    @Operation(summary = "Cria um usuário (ADMIN pode definir o perfil)")
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest dto) {
        UsuarioResponse criado = UsuarioResponse.from(service.criar(dto.toEntity()));
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar().stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return UsuarioResponse.from(service.buscar(id));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id, @Valid @RequestBody UsuarioUpdateRequest dto) {
        return UsuarioResponse.from(service.atualizar(id, dto.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
