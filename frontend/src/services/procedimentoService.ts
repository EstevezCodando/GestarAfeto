import clienteApi from '../api/clienteApi'
import type { Procedimento, CriarProcedimentoRequest } from '../types/Procedimento'

export const procedimentoService = {
  listar: () => clienteApi.get<Procedimento[]>('/procedimentos').then(r => r.data),
  buscar: (id: number) => clienteApi.get<Procedimento>(`/procedimentos/${id}`).then(r => r.data),
  cadastrar: (data: CriarProcedimentoRequest) => clienteApi.post<Procedimento>('/procedimentos', data).then(r => r.data),
  atualizar: (id: number, data: CriarProcedimentoRequest) => clienteApi.put<Procedimento>(`/procedimentos/${id}`, data).then(r => r.data),
  remover: (id: number) => clienteApi.delete(`/procedimentos/${id}`),
}
