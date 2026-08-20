export interface Gestante {
  id: number
  nome: string
  dataNascimento: string | null
  telefone: string | null
  email: string | null
  dataUltimaMenstruacao: string | null
  dataProvavelParto: string | null
  observacoes: string | null
  dataCadastro: string
}

export interface CriarGestanteRequest {
  nome: string
  dataNascimento?: string | null
  telefone?: string | null
  email?: string | null
  dataUltimaMenstruacao?: string | null
  dataProvavelParto?: string | null
  observacoes?: string | null
}
