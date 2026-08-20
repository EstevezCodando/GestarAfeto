import clienteApi from '../api/clienteApi'
import type { Alerta, ResumoAlertas } from '../types/Alerta'

/**
 * O front-end conversa apenas com o servico principal, que encaminha as chamadas
 * ao microsservico de alertas. Isso mantem uma unica origem HTTP no navegador.
 */
export const alertaService = {
  /** Recalcula os alertas a partir do estado atual do checklist. */
  avaliar: (gestanteId: number) =>
    clienteApi.post<Alerta[]>(`/gestantes/${gestanteId}/alertas/avaliar`).then(r => r.data),
  listar: (gestanteId: number) =>
    clienteApi.get<Alerta[]>(`/gestantes/${gestanteId}/alertas`).then(r => r.data),
  resumo: (gestanteId: number) =>
    clienteApi.get<ResumoAlertas>(`/gestantes/${gestanteId}/alertas/resumo`).then(r => r.data),
  marcarLido: (alertaId: number) =>
    clienteApi.patch<Alerta>(`/alertas/${alertaId}/leitura`).then(r => r.data),
  resolver: (alertaId: number) =>
    clienteApi.patch<Alerta>(`/alertas/${alertaId}/resolucao`).then(r => r.data),
}
