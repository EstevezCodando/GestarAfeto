import clienteApi from '../api/clienteApi'
import type { ItemChecklist, AtualizarStatusRequest } from '../types/Checklist'

export const checklistService = {
  gerar: (gestanteId: number) =>
    clienteApi.post<ItemChecklist[]>(`/gestantes/${gestanteId}/checklist/gerar`).then(r => r.data),
  listar: (gestanteId: number) =>
    clienteApi.get<ItemChecklist[]>(`/gestantes/${gestanteId}/checklist`).then(r => r.data),
  atualizarStatus: (itemId: number, data: AtualizarStatusRequest) =>
    clienteApi.patch<ItemChecklist>(`/checklist/${itemId}/status`, data).then(r => r.data),
  marcarRealizado: (itemId: number) =>
    clienteApi.patch<ItemChecklist>(`/checklist/${itemId}/realizar`).then(r => r.data),
  marcarParaRevisar: (itemId: number) =>
    clienteApi.patch<ItemChecklist>(`/checklist/${itemId}/revisar`).then(r => r.data),
}
