<script setup>
import { ref } from 'vue'
import { registrarContribuicao, consultarRanking, encerrarMes } from '../api'

const descricao = ref('')
const alunoNome = ref('')
const peso = ref('1')

const carregandoContribuicao = ref(false)
const carregandoRanking = ref(false)
const carregandoEncerrar = ref(false)

const mensagemContribuicao = ref('')
const tipoContribuicao = ref('sucesso')

const ranking = ref(null)
const erroRanking = ref('')

const vencedor = ref(null)
const erroEncerrar = ref('')

async function registrar() {
  if (!descricao.value.trim() || !alunoNome.value.trim()) {
    mensagemContribuicao.value = 'Informe a descricao do mes e o nome do aluno.'
    tipoContribuicao.value = 'erro'
    return
  }
  carregandoContribuicao.value = true
  mensagemContribuicao.value = ''
  try {
    const resultado = await registrarContribuicao(descricao.value.trim(), alunoNome.value.trim(), Number(peso.value))
    if (resultado.registrada) {
      mensagemContribuicao.value = 'Contribuicao registrada.'
      tipoContribuicao.value = 'sucesso'
    } else {
      mensagemContribuicao.value = 'Contribuicao ignorada: esse aluno acabou de contribuir (contribuicao consecutiva).'
      tipoContribuicao.value = 'erro'
    }
  } catch (e) {
    mensagemContribuicao.value = e.message || 'Ocorreu um erro.'
    tipoContribuicao.value = 'erro'
  } finally {
    carregandoContribuicao.value = false
  }
}

async function verRanking() {
  if (!descricao.value.trim()) {
    erroRanking.value = 'Informe a descricao do mes.'
    return
  }
  carregandoRanking.value = true
  erroRanking.value = ''
  try {
    ranking.value = await consultarRanking(descricao.value.trim())
  } catch (e) {
    erroRanking.value = e.message || 'Ocorreu um erro.'
    ranking.value = null
  } finally {
    carregandoRanking.value = false
  }
}

async function encerrar() {
  if (!descricao.value.trim()) {
    erroEncerrar.value = 'Informe a descricao do mes.'
    return
  }
  carregandoEncerrar.value = true
  erroEncerrar.value = ''
  try {
    vencedor.value = await encerrarMes(descricao.value.trim())
  } catch (e) {
    erroEncerrar.value = e.message || 'Ocorreu um erro.'
    vencedor.value = null
  } finally {
    carregandoEncerrar.value = false
  }
}
</script>

<template>
  <section class="card">
    <h2>Mes de Contribuicoes</h2>
    <h3>Registrar contribuicoes no forum, ver o ranking e encerrar o mes</h3>

    <div class="campo" style="margin-bottom: 14px;">
      <label for="descricao-mes">Descricao do mes (identifica o mes, ex: set-2026)</label>
      <input id="descricao-mes" v-model="descricao" placeholder="ex: set-2026" style="max-width: 220px;" />
    </div>

    <form class="linha" @submit.prevent="registrar">
      <div class="campo">
        <label for="aluno-mes">Nome do aluno</label>
        <input id="aluno-mes" v-model="alunoNome" placeholder="ex: Juan" />
      </div>
      <div class="campo">
        <label for="peso-mes">Peso da contribuicao</label>
        <input id="peso-mes" v-model="peso" type="number" step="1" style="min-width: 90px;" />
      </div>
      <button class="acao" type="submit" :disabled="carregandoContribuicao">
        {{ carregandoContribuicao ? 'Enviando...' : 'Registrar contribuicao' }}
      </button>
    </form>
    <p v-if="mensagemContribuicao" class="mensagem" :class="tipoContribuicao">{{ mensagemContribuicao }}</p>
  </section>

  <section class="card">
    <div class="card-header">
      <h2>Ranking do mes</h2>
      <button class="acao secundaria" @click="verRanking" :disabled="carregandoRanking">
        {{ carregandoRanking ? 'Buscando...' : 'Ver ranking' }}
      </button>
    </div>
    <p v-if="erroRanking" class="mensagem erro">{{ erroRanking }}</p>
    <p v-if="ranking && ranking.contribuicoes.length === 0" class="vazio">
      Nenhuma contribuicao registrada nesse mes ainda.
    </p>
    <table v-if="ranking && ranking.contribuicoes.length > 0">
      <thead>
        <tr>
          <th>#</th>
          <th>Aluno</th>
          <th>Pontos</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, indice) in ranking.contribuicoes" :key="indice">
          <td>{{ indice + 1 }}</td>
          <td>{{ item.alunoNome }}</td>
          <td>{{ item.pontos }}</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section class="card">
    <div class="card-header">
      <h2>Encerrar mes</h2>
      <button class="acao secundaria" @click="encerrar" :disabled="carregandoEncerrar">
        {{ carregandoEncerrar ? 'Encerrando...' : 'Encerrar mes' }}
      </button>
    </div>
    <p style="color: var(--cor-texto-suave); font-size: 0.85rem; margin-top: -4px;">
      Apura quem teve mais contribuicoes no mes (com sorteio em caso de empate) e concede um curso ao vencedor.
    </p>
    <p v-if="erroEncerrar" class="mensagem erro">{{ erroEncerrar }}</p>
    <p v-if="vencedor && !vencedor.alunoVencedor" class="vazio">Nenhuma contribuicao neste mes, ninguem venceu.</p>
    <p v-if="vencedor && vencedor.alunoVencedor" class="mensagem sucesso">
      Vencedor: <strong>{{ vencedor.alunoVencedor }}</strong> - agora com {{ vencedor.cursosConcluidos }} curso(s) concluido(s).
    </p>
  </section>
</template>
