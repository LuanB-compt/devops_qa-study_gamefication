package br.com.valueprojects.gamification.contribuicao.controller;

import br.com.valueprojects.gamification.contribuicao.dto.RankingMesResponse;
import br.com.valueprojects.gamification.contribuicao.dto.RegistrarContribuicaoRequest;
import br.com.valueprojects.gamification.contribuicao.dto.VencedorResponse;
import br.com.valueprojects.gamification.contribuicao.service.MesDeContribuicoesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do mês de contribuições: registrar contribuição no fórum
 * (cenário 4), consultar o ranking do mês e encerrar o mês para apurar o
 * vencedor e conceder o curso (cenários 1 e 3).
 */
@RestController
@RequestMapping("/api/meses")
public class MesDeContribuicoesController {

    private final MesDeContribuicoesService mesDeContribuicoesService;

    public MesDeContribuicoesController(MesDeContribuicoesService mesDeContribuicoesService) {
        this.mesDeContribuicoesService = mesDeContribuicoesService;
    }

    @PostMapping("/{descricao}/contribuicoes")
    public ResponseEntity<Void> registrarContribuicao(@PathVariable String descricao,
            @RequestBody RegistrarContribuicaoRequest request) {
        boolean registrada = mesDeContribuicoesService.registrarContribuicao(
                descricao, request.alunoNome(), request.peso());

        // Cenário 4: contribuição consecutiva do mesmo aluno é ignorada -> 409.
        return registrada
                ? ResponseEntity.status(HttpStatus.CREATED).build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @GetMapping("/{descricao}/ranking")
    public RankingMesResponse ranking(@PathVariable String descricao) {
        return mesDeContribuicoesService.consultarRanking(descricao);
    }

    @PostMapping("/{descricao}/encerrar")
    public VencedorResponse encerrar(@PathVariable String descricao) {
        return mesDeContribuicoesService.encerrarMes(descricao);
    }
}
