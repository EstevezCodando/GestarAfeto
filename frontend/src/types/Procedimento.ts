export type TipoProcedimento = 'CONSULTA' | 'EXAME' | 'VACINA' | 'ORIENTACAO' | 'ULTRASSONOGRAFIA'
export type TrimestreGestacional = 'PRIMEIRO' | 'SEGUNDO' | 'TERCEIRO'

export interface Procedimento {
  id: number
  nome: string
  descricao: string | null
  tipo: TipoProcedimento
  trimestreRecomendado: TrimestreGestacional | null
  semanaInicialRecomendada: number | null
  semanaFinalRecomendada: number | null
  obrigatorio: boolean
  ativo: boolean
}

export interface CriarProcedimentoRequest {
  nome: string
  descricao?: string | null
  tipo: TipoProcedimento
  trimestreRecomendado?: TrimestreGestacional | null
  semanaInicialRecomendada?: number | null
  semanaFinalRecomendada?: number | null
  obrigatorio: boolean
}
