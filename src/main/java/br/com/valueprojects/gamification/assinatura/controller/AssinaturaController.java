package br.com.valueprojects.gamification.assinatura.controller;

import br.com.valueprojects.gamification.assinatura.dto.AssinaturaResponse;
import br.com.valueprojects.gamification.assinatura.service.AssinaturaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ativação, pagamento, encerramento de mês e consulta da assinatura de um
 * aluno (cenário 2).
 */
@RestController
@RequestMapping("/api/assinaturas")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    public AssinaturaController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @PostMapping("/{alunoNome}")
    public AssinaturaResponse criar(@PathVariable String alunoNome) {
        return assinaturaService.criar(alunoNome);
    }

    @GetMapping("/{alunoNome}")
    public AssinaturaResponse consultar(@PathVariable String alunoNome) {
        return assinaturaService.consultar(alunoNome);
    }

    @PostMapping("/{alunoNome}/pagamento")
    public AssinaturaResponse registrarPagamento(@PathVariable String alunoNome) {
        return assinaturaService.registrarPagamento(alunoNome);
    }

    @PostMapping("/{alunoNome}/encerrar")
    public AssinaturaResponse encerrarMes(@PathVariable String alunoNome) {
        return assinaturaService.encerrarMes(alunoNome);
    }
}
