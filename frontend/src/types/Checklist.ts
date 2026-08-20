import type { TipoProcedimento } from './Procedimento'

export type StatusChecklist = 'PENDENTE' | 'REALIZADO' | 'NAO_SE_APLICA' | 'PRECISA_REVISAR'

export interface ProcedimentoResumo {
  id: number
  nome: string
  tipo: TipoProcedimento
}

export interface ItemChecklist {
  id: number
  procedimento: ProcedimentoResumo
  status: StatusChecklist
  dataPrevista: string | null
  dataRealizacao: string | null
  observacao: string | null
  dataCriacao: string
}

export interface AtualizarStatusRequest {
  status: StatusChecklist
  dataRealizacao?: string | null
  observacao?: string | null
}
