export type TipoAlerta =
  | 'PROCEDIMENTO_ATRASADO'
  | 'PROCEDIMENTO_PENDENTE'
  | 'JANELA_PROXIMA'
  | 'REVISAO_SOLICITADA'

export type PrioridadeAlerta = 'ALTA' | 'MEDIA' | 'BAIXA'

export type StatusAlerta = 'ABERTO' | 'LIDO' | 'RESOLVIDO' | 'CANCELADO'

export interface Alerta {
  id: number
  gestanteId: number
  gestanteNome: string
  /** Id do item de checklist que originou o alerta. */
  origemId: number
  tipo: TipoAlerta
  prioridade: PrioridadeAlerta
  status: StatusAlerta
  titulo: string
  mensagem: string
  dataReferencia: string | null
  semanaGestacionalReferencia: number | null
  dataCriacao: string
  dataLeitura: string | null
  dataResolucao: string | null
}

export interface ResumoAlertas {
  gestanteId: number
  totalAtivos: number
  alta: number
  media: number
  baixa: number
  naoLidos: number
  resolvidos: number
}

/**
 * Resposta do HTTP 202 da reavaliacao. O servico apenas publicou o evento: o
 * calculo acontece depois, no microsservico.
 */
export interface SolicitacaoAceita {
  gestanteId: number
  status: string
  mensagem: string
}
