import clienteApi from '../api/clienteApi'
import type { Alerta, ResumoAlertas, SolicitacaoAceita } from '../types/Alerta'

/**
 * O front-end conversa apenas com o servico principal, que encaminha as chamadas
 * ao microsservico de alertas. Isso mantem uma unica origem HTTP no navegador.
 */
export const alertaService = {
  /**
   * Solicita a reavaliacao dos alertas. Responde 202: o servico publicou o evento e
   * o calculo acontece de forma assincrona, entao a lista atualizada vem de uma
   * consulta posterior, e nao desta chamada.
   */
  avaliar: (gestanteId: number) =>
    clienteApi.post<SolicitacaoAceita>(`/gestantes/${gestanteId}/alertas/avaliar`).then(r => r.data),
  listar: (gestanteId: number) =>
    clienteApi.get<Alerta[]>(`/gestantes/${gestanteId}/alertas`).then(r => r.data),
  resumo: (gestanteId: number) =>
    clienteApi.get<ResumoAlertas>(`/gestantes/${gestanteId}/alertas/resumo`).then(r => r.data),
  marcarLido: (alertaId: number) =>
    clienteApi.patch<Alerta>(`/alertas/${alertaId}/leitura`).then(r => r.data),
  resolver: (alertaId: number) =>
    clienteApi.patch<Alerta>(`/alertas/${alertaId}/resolucao`).then(r => r.data),
}
