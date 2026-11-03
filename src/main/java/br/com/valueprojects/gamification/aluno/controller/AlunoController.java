package br.com.valueprojects.gamification.aluno.controller;

import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.service.AlunoService;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alunos")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @GetMapping
    public List<AlunoResponse> listar() {
        return alunoService.listarTodos().stream()
                .map(alunoService::paraResponse)
                .toList();
    }

    @GetMapping("/{nome}")
    public AlunoResponse buscar(@PathVariable String nome) {
        return alunoService.paraResponse(alunoService.buscarPorNome(nome));
    }
}
