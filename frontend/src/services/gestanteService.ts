import clienteApi from '../api/clienteApi'
import type { Gestante, CriarGestanteRequest } from '../types/Gestante'

export const gestanteService = {
  listar: () => clienteApi.get<Gestante[]>('/gestantes').then(r => r.data),
  buscar: (id: number) => clienteApi.get<Gestante>(`/gestantes/${id}`).then(r => r.data),
  cadastrar: (data: CriarGestanteRequest) => clienteApi.post<Gestante>('/gestantes', data).then(r => r.data),
  atualizar: (id: number, data: CriarGestanteRequest) => clienteApi.put<Gestante>(`/gestantes/${id}`, data).then(r => r.data),
  remover: (id: number) => clienteApi.delete(`/gestantes/${id}`),
}
