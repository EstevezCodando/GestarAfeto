import axios from 'axios'

const clienteApi = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

clienteApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const mensagem = error.response?.data?.mensagem ?? error.message ?? 'Erro inesperado'
    return Promise.reject(new Error(mensagem))
  }
)

export default clienteApi
