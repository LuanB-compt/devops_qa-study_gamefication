package br.com.valueprojects.gamification.contribuicao.dto;

import java.util.List;

public record RankingMesResponse(String descricao, List<ContribuicaoResponse> contribuicoes) {
}
