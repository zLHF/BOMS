import http from './request'
import type { AttachmentRow, SignUploadReq, SignUploadResp } from '@/types'

const get = <T>(url: string, params?: object) => http.get(url, { params }) as unknown as Promise<T>
const post = <T>(url: string, body?: object) => http.post(url, body) as unknown as Promise<T>
const del = <T>(url: string) => http.delete(url) as unknown as Promise<T>

/* ---------- M16 附件中心 ---------- */
export const fileApi = {
  /** 获取预签名上传 URL */
  signUpload: (req: SignUploadReq) => post<SignUploadResp>('/files/sign-upload', req),
  /** 确认上传完成 */
  confirm: (attachmentId: number) => post<void>('/files', { attachmentId }),
  /** 获取预签名下载 URL */
  signDownload: (id: number) => get<string>(`/files/${id}/sign-download`),
  /** 删除附件 */
  remove: (id: number) => del<void>(`/files/${id}`),
  /** 按业务对象查询附件列表 */
  list: (objectType: string, objectId: number) => get<AttachmentRow[]>('/files', { objectType, objectId }),
}
