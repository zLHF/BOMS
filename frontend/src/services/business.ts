import http from './request'
import type { Page, CustomerRow, ContactRow, OpportunityRow, StageRow, FollowRow, AuditLogRow, TaskRow, NumberRuleRow, OpportunityDetailVO, CollaboratorRow, PoolConfigRow, FollowListRow, ImportProgressResp } from '@/types'

const get = <T>(url: string, params?: object) => http.get(url, { params }) as unknown as Promise<T>
const post = <T>(url: string, body?: object) => http.post(url, body) as unknown as Promise<T>
const put = <T>(url: string, body?: object) => http.put(url, body) as unknown as Promise<T>
const del = <T>(url: string) => http.delete(url) as unknown as Promise<T>

/* ---------- M10 客户 ---------- */
export const customerApi = {
  list: (params: object) => get<Page<CustomerRow>>('/customers', params),
  get: (id: number) => get<CustomerRow>(`/customers/${id}`),
  create: (body: object) => post<CustomerRow>('/customers', body),
  update: (id: number, body: object) => put<void>(`/customers/${id}`, body),
  remove: (id: number) => del<void>(`/customers/${id}`),
  contacts: (id: number) => get<ContactRow[]>(`/customers/${id}/contacts`),
  addContact: (id: number, body: object) => post<ContactRow>(`/customers/${id}/contacts`, body),
  delContact: (id: number, cid: number) => del<void>(`/customers/${id}/contacts/${cid}`),
}

/* ---------- M05/M06/M07 商机 ---------- */
export const oppApi = {
  list: (params: object) => get<Page<OpportunityRow>>('/opportunities', params),
  get: (id: number) => get<OpportunityRow>(`/opportunities/${id}`),
  detail: (id: number) => get<OpportunityDetailVO>(`/opportunities/${id}/detail`),
  create: (body: object) => post<OpportunityRow>('/opportunities', body),
  update: (id: number, body: object) => put<void>(`/opportunities/${id}`, body),
  remove: (id: number) => del<void>(`/opportunities/${id}`),
  moveStage: (id: number, stageId: number) => post<void>(`/opportunities/${id}/stage`, { stageId }),
  rollback: (id: number, stageId: number) => post<void>(`/opportunities/${id}/stage/rollback`, { stageId }),
  win: (id: number, body: object) => post<void>(`/opportunities/${id}/win`, body),
  lose: (id: number, body: object) => post<void>(`/opportunities/${id}/lose`, body),
  follows: (id: number) => get<FollowRow[]>(`/opportunities/${id}/follows`),
  addFollow: (id: number, body: object) => post<FollowRow>(`/opportunities/${id}/follows`, body),
  contacts: (id: number) => get<ContactRow[]>(`/opportunities/${id}/contacts`),
  collaborators: (id: number) => get<CollaboratorRow[]>(`/opportunities/${id}/collaborators`),
  addCollaborator: (id: number, body: object) => post<CollaboratorRow>(`/opportunities/${id}/collaborators`, body),
  removeCollaborator: (id: number, cid: number) => del<void>(`/opportunities/${id}/collaborators/${cid}`),
  /* 批量导入 */
  importTemplate: () => '/opportunities/import/template',
  startImport: (formData: FormData) => http.post('/opportunities/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }) as unknown as Promise<ImportProgressResp>,
  importProgress: (taskId: number) => get<ImportProgressResp>(`/opportunities/import/${taskId}`),
}

export const configApi = {
  stages: () => get<StageRow[]>('/config/stages'),
  updateStage: (id: number, body: object) => put<void>(`/config/stages/${id}`, body),
  numberRules: () => get<NumberRuleRow[]>('/config/number-rules'),
  updateNumberRule: (bizType: string, body: object) => put<void>(`/config/number-rules/${bizType}`, body),
}

/* ---------- M12 任务 ---------- */
export const taskApi = {
  list: (params: object) => get<Page<TaskRow>>('/tasks', params),
  create: (body: object) => post<TaskRow>('/tasks', body),
  update: (id: number, body: object) => put<void>(`/tasks/${id}`, body),
  assign: (id: number, assigneeId: number) => post<void>(`/tasks/${id}/assign`, { assigneeId }),
  cancel: (id: number) => post<void>(`/tasks/${id}/cancel`),
}

/* ---------- 审计日志 ---------- */
export const auditApi = {
  list: (params: object) => get<Page<AuditLogRow>>('/audit-logs', params),
}

/* ---------- 公海池 ---------- */
export const poolApi = {
  opportunities: (params: object) => get<Page<OpportunityRow>>('/pool/opportunities', params),
  customers: (params: object) => get<Page<CustomerRow>>('/pool/customers', params),
  claimOpportunity: (id: number) => post<void>(`/pool/opportunities/${id}/claim`),
  claimCustomer: (id: number) => post<void>(`/pool/customers/${id}/claim`),
  recycleOpportunity: (id: number, reason: string) => post<void>(`/pool/opportunities/${id}/recycle`, { reason }),
  recycleCustomer: (id: number, reason: string) => post<void>(`/pool/customers/${id}/recycle`, { reason }),
}

/* ---------- 公海池配置 ---------- */
export const poolConfigApi = {
  get: () => get<PoolConfigRow>('/pool/config'),
  update: (body: object) => put<void>('/pool/config', body),
  execute: () => post<string>('/pool/config/execute'),
}

/* ---------- 跟进记录独立页 ---------- */
export const followsApi = {
  list: (params: object) => get<Page<FollowListRow>>('/follows', params),
}
