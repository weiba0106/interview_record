import { request } from '@/shared/api/http'
import type { Company, CompanyDetail, CompanyRequest } from './tracking.types'

export async function listCompanies(): Promise<Company[]> {
  return (await request<Company[]>({ method: 'get', url: '/companies' })).data
}

export async function getCompany(id: string): Promise<CompanyDetail> {
  return (await request<CompanyDetail>({ method: 'get', url: `/companies/${id}` })).data
}

export async function createCompany(payload: CompanyRequest): Promise<Company> {
  return (await request<Company>({ method: 'post', url: '/companies', data: payload })).data
}

export async function updateCompany(id: string, payload: CompanyRequest): Promise<Company> {
  return (await request<Company>({ method: 'put', url: `/companies/${id}`, data: payload })).data
}

export async function deleteCompany(id: string, confirmed: boolean): Promise<void> {
  await request({ method: 'delete', url: `/companies/${id}`, params: { confirmed } })
}
