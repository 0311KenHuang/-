import { http } from './http';

export interface LoginPayload {
  username: string;
  password: string;
  tenantCode?: string;
}

export interface LoginResult {
  token: string;
  user: {
    id: number;
    username: string;
    realName: string;
    orgId: number | null;
    roles: string[];
    dataScope: string;
  };
}

export const authApi = {
  login: (payload: LoginPayload) => http.post<LoginResult>('/auth/login', payload),
  profile: () => http.get<Record<string, unknown>>('/auth/profile'),
};

export interface Station {
  id: number;
  stationCode: string;
  name: string;
  stationType: string;
  enterpriseId: number;
  bizStatus: string;
  opsEngineerId: number | null;
  lng?: string | null;
  lat?: string | null;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

export const stationApi = {
  page: (params: Record<string, unknown>) => http.get<PageResult<Station>>('/station', params),
  detail: (id: number) => http.get<Station>(`/station/${id}`),
  create: (data: Partial<Station>) => http.post<Station>('/station', data),
  changeStatus: (id: number, bizStatus: string, stopReason?: string) =>
    http.patch<Station>(`/station/${id}/status`, { bizStatus, stopReason }),
};

export interface Overview {
  stationTotal: number;
  stationByStatus: Record<string, number>;
  alarmTotal: number;
  /** 未结束的报警数（OPEN/ACK/PROCESSING） */
  alarmOpenTotal: number;
  frameToday: number;
  lastFrameAt: string | null;
  /** 今日分钟数据条数 */
  minuteToday: number;
  /** 今日分钟数据有效率 0~1 */
  minuteValidRate: number;
}

export interface AlarmEvent {
  id: number;
  eventNo: string;
  ruleId: number;
  ruleCode: string | null;
  ruleName: string | null;
  stationId: number;
  stationCode: string | null;
  stationName: string | null;
  factorCode: string | null;
  level: string;
  value: number | null;
  threshold: number | null;
  beginAt: string;
  endAt: string | null;
  status: string;
  ownerId: number | null;
}

export interface RealTimeValue {
  factorCode: string;
  unit: string | null;
  value: number | null;
  ts: string | null;
  devFlag: string | null;
  /** 1 有效 / 2 无效 / 3 未标记 */
  quality: number | null;
  stdLimit: number | null;
}

export interface StationRealtime {
  stationId: number;
  stationCode: string;
  stationName: string;
  bizStatus: string;
  updatedAt: string | null;
  factors: RealTimeValue[];
}

export const monitorApi = {
  overview: () => http.get<Overview>('/monitor/overview'),
  realtime: (limit = 200) => http.get<StationRealtime[]>('/monitor/realtime', { limit }),
};

export const alarmApi = {
  page: (params: Record<string, unknown>) => http.get<PageResult<AlarmEvent>>('/alarm/events', params),
  handle: (id: number, action: string, remark?: string) =>
    http.patch<AlarmEvent>(`/alarm/events/${id}/handle`, { action, remark }),
};

export interface MsgSendLog {
  id: number;
  eventId: number | null;
  channel: string;
  receiver: string | null;
  templateCode: string | null;
  /** SENT / FAILED / SKIPPED / RETRY */
  status: string;
  stage: number;
  retryCnt: number;
  errorMsg: string | null;
  content: string | null;
  sentAt: string;
}

export const messageApi = {
  logs: (params: Record<string, unknown>) => http.get<PageResult<MsgSendLog>>('/message/logs', params),
  channels: () => http.get<Record<string, { configured: boolean; reason?: string }>>('/message/channels'),
};

export interface OpsTaskView {
  id: number;
  taskNo: string;
  stationId: number;
  stationName: string | null;
  opsType: string;
  source: string;
  assigneeId: number;
  assigneeName: string | null;
  dispatchAt: string | null;
  acceptAt: string | null;
  arriveAt: string | null;
  finishAt: string | null;
  deadline: string | null;
  status: string;
  isOverdue: number;
}

export interface OpsPlan {
  id: number;
  planNo: string;
  stationId: number;
  period: string;
  status: string;
}

export interface OpsRecord {
  id: number;
  recordNo: string;
  taskId: number;
  stationId: number;
  opsType: string;
  status: string;
  beginTime: string;
}

export interface OpsCoverage {
  period: string;
  totalStations: number;
  plannedStations: number;
  coverageRate: number;
  unplannedStationIds: number[];
}

export interface DevParamChange {
  id: number;
  stationId: number;
  deviceId: number | null;
  paramCode: string;
  oldValue: string | null;
  newValue: string | null;
  changeTime: string;
  eventId: number | null;
}

export const deviceApi = {
  changes: (params: Record<string, unknown>) => http.get<DevParamChange[]>('/device/params/changes', params),
  latest: (stationId: number) => http.get<Record<string, unknown>[]>('/device/params/latest', { stationId }),
};

export const opsApi = {
  tasks: (params: Record<string, unknown>) => http.get<PageResult<OpsTaskView>>('/ops/task', params),
  act: (id: number, data: Record<string, unknown>) => http.patch<OpsTaskView>(`/ops/task/${id}/action`, data),
  plans: (period: string) => http.get<OpsPlan[]>('/ops/plan', { period }),
  generate: (period: string, opsTypes: string[]) =>
    http.post<Record<string, unknown>>('/ops/plan/generate', { period, opsTypes }),
  publish: (id: number) => http.post<Record<string, unknown>>(`/ops/plan/${id}/publish`, {}),
  coverage: (period: string) => http.get<OpsCoverage>('/ops/plan/coverage', { period }),
  pendingRecords: () => http.get<OpsRecord[]>('/ops/record/pending', { limit: 20 }),
  review: (id: number, result: string, reason?: string) =>
    http.post<OpsRecord>(`/ops/audit/${id}`, { result, reason }),
  trace: (from: string, to: string) =>
    http.get<{ fakeMarks: string[]; missingMarks: string[] }>('/ops/trace/check', { from, to }),
  submitRecord: (data: Record<string, unknown>) => http.post<OpsRecord>('/ops/record', data),
  submitForAudit: (id: number) => http.post<OpsRecord>(`/ops/record/${id}/submit`, {}),
  createTaskFromAlarm: (id: number) => http.post<OpsTaskView>(`/alarm/events/${id}/task`, {}),
};
