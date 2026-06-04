/** 后端统一响应包装。 */
export interface R<T> {
  code: number
  message: string
  data: T
}

export interface LoginResult {
  token: string
  userId: number
  tenantId: number
  username: string
  realName: string
  permissions: string[]
}

export interface MeResult {
  userId: number
  tenantId: number
  username: string
  realName: string
  isPlatform: boolean
  permissions: string[]
  roles: string[]
  dataScope: string
}

export interface Page<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface TenantRow {
  id: number
  name: string
  code: string
  domain?: string
  status: string
  packageId?: number
  expireAt?: string
}

export interface SysPackageRow {
  id: number
  name: string
  maxUsers: number
  maxOpportunities: number
  maxCustomers: number
}

export interface DeptNode {
  id: number
  parentId: number
  name: string
  leaderId?: number
  sort: number
  status: string
  children: DeptNode[]
}

export interface UserRow {
  id: number
  tenantId: number
  username: string
  mobile?: string
  email?: string
  realName: string
  deptId?: number
  status: string
}

export interface RoleRow {
  id: number
  name: string
  code: string
  dataScope: string
  status: string
  remark?: string
}

export interface PermissionRow {
  id: number
  code: string
  type: string
  name: string
  module?: string
}

export interface CustomerRow {
  id: number
  name: string
  creditCode?: string
  industry?: string
  region?: string
  level?: string
  ownerId?: number
  status: string
  parentId: number
}

export interface ContactRow {
  id: number
  customerId: number
  name: string
  title?: string
  mobile?: string
  email?: string
  isKeyPerson: number
}

export interface StageRow {
  id: number
  code: string
  name: string
  sort: number
  winRate: number
  color?: string
  stageType: string
  isActive?: number
}

export interface OpportunityRow {
  id: number
  title: string
  customerId: number
  stageId: number
  amount: number
  winRate: number
  ownerId?: number
  status: string
  source?: string
  demand?: string
  dealAmount?: number
  dealAt?: string
  expectedCloseAt?: string
  lastFollowAt?: string
  nextFollowAt?: string
}

export interface FollowRow {
  id: number
  opportunityId: number
  followType?: string
  content?: string
  result?: string
  nextTime?: string
  createdAt: string
}

/** 跟进列表独立页行类型（含富化字段）。 */
export interface FollowListRow {
  id: number
  opportunityId: number
  opportunityTitle: string
  followType?: string
  content?: string
  result?: string
  nextTime?: string
  creatorId?: number
  creatorName?: string
  createdAt: string
}

export interface AuditLogRow {
  id: number
  userId?: number
  userName?: string
  objectType?: string
  objectId?: number
  action: string
  beforeJson?: string
  afterJson?: string
  result: string
  ip?: string
  createdAt: string
}

export interface TaskRow {
  id: number
  objectType?: string
  objectId?: number
  title: string
  content?: string
  assigneeId?: number
  creatorId?: number
  dueAt?: string
  priority: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface NumberRuleRow {
  id: number
  bizType: string
  prefix?: string
  dateFormat?: string
  seqLength: number
  currentSeq: number
}

export interface OpportunityDetailVO {
  opportunity: OpportunityRow
  customerName: string
  stageName: string
  ownerName: string
}

export interface CollaboratorRow {
  id: number
  opportunityId: number
  userId: number
  permissionJson?: string
  status: string
}

/** 附件行类型。 */
export interface AttachmentRow {
  id: number
  fileName: string
  fileSize: number
  contentType: string
  objectType: string
  objectId: number
  status: string
  uploaderId: number
  createdAt: string
}

/** 签名上传请求。 */
export interface SignUploadReq {
  fileName: string
  contentType: string
  objectType: string
  objectId: number
  fileSize: number
}

/** 签名上传响应。 */
export interface SignUploadResp {
  attachmentId: number
  uploadUrl: string
  headers: Record<string, string>
}

/** 公海池配置。 */
export interface PoolConfigRow {
  id: number
  autoRecycleEnabled: number
  noFollowDays: number
  protectionDays: number
  personalLimit: number
}

/** 数据字典项。 */
export interface DictRow {
  id: number
  tenantId: number
  dictType: string
  itemCode: string
  itemLabel: string
  sort: number
  isActive: number
}

/** 导入进度响应。 */
export interface ImportProgressResp {
  taskId: number
  status: string
  total: number
  success: number
  failed: number
}
