import http from './request'
import type { Page, TenantRow, SysPackageRow, DeptNode, UserRow, RoleRow, PermissionRow } from '@/types'

const get = <T>(url: string, params?: object) => http.get(url, { params }) as unknown as Promise<T>
const post = <T>(url: string, body?: object) => http.post(url, body) as unknown as Promise<T>
const put = <T>(url: string, body?: object) => http.put(url, body) as unknown as Promise<T>
const patch = <T>(url: string, body?: object) => http.patch(url, body) as unknown as Promise<T>
const del = <T>(url: string) => http.delete(url) as unknown as Promise<T>

/* ---------- M02 租户（平台级） ---------- */
export const tenantApi = {
  list: () => get<TenantRow[]>('/platform/tenants'),
  packages: () => get<SysPackageRow[]>('/platform/packages'),
  create: (body: object) => post<{ tenantId: number; adminUsername: string; adminPassword: string }>('/platform/tenants', body),
  update: (id: number, body: object) => put<void>(`/platform/tenants/${id}`, body),
  status: (id: number, status: string) => patch<void>(`/platform/tenants/${id}/status`, { status }),
  changePackage: (id: number, packageId: number) => put<void>(`/platform/tenants/${id}/package`, { packageId }),
}

/* ---------- M03 部门/用户 ---------- */
export const deptApi = {
  tree: () => get<DeptNode[]>('/depts'),
  create: (body: object) => post<DeptNode>('/depts', body),
  update: (id: number, body: object) => put<void>(`/depts/${id}`, body),
  remove: (id: number) => del<void>(`/depts/${id}`),
}

export const userApi = {
  list: (params: object) => get<Page<UserRow>>('/users', params),
  roles: (id: number) => get<number[]>(`/users/${id}/roles`),
  create: (body: object) => post<{ id: number; username: string }>('/users', body),
  update: (id: number, body: object) => put<void>(`/users/${id}`, body),
  disable: (id: number) => patch<void>(`/users/${id}/disable`),
  resetPwd: (id: number) => post<{ password: string }>(`/users/${id}/reset-password`),
  assignRoles: (id: number, roleIds: number[]) => put<void>(`/users/${id}/roles`, { roleIds }),
}

/* ---------- M04 角色/权限 ---------- */
export const roleApi = {
  list: () => get<RoleRow[]>('/roles'),
  permissions: () => get<PermissionRow[]>('/permissions'),
  rolePerms: (id: number) => get<number[]>(`/roles/${id}/permissions`),
  create: (body: object) => post<RoleRow>('/roles', body),
  update: (id: number, body: object) => put<void>(`/roles/${id}`, body),
  remove: (id: number) => del<void>(`/roles/${id}`),
  assignPerms: (id: number, permissionIds: number[], dataScope: string) =>
    put<void>(`/roles/${id}/permissions`, { permissionIds, dataScope }),
}
