<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  alarmApi,
  deviceApi,
  messageApi,
  monitorApi,
  type AlarmEvent,
  type DevParamChange,
  type MsgSendLog,
  type Overview,
  type StationRealtime,
} from '@/api';

interface RealtimeRow {
  stationName: string;
  factorCode: string;
  unit: string | null;
  value: number | null;
  ts: string | null;
  devFlag: string | null;
  quality: number | null;
}

const loading = ref(false);
const data = ref<Overview | null>(null);
const realtime = ref<StationRealtime[]>([]);
const alarms = ref<AlarmEvent[]>([]);
const msgLogs = ref<MsgSendLog[]>([]);
const paramChanges = ref<DevParamChange[]>([]);

const cards = [
  { key: 'stationTotal', label: '站房总数', color: '#1f6feb' },
  { key: 'dataValidRate', label: '数据有效率', color: '#067a5b' },
  { key: 'alarmTotal', label: '报警事件', color: '#b91c1c' },
  { key: 'frameToday', label: '今日报文', color: '#6d28d9' },
] as const;

const rows = computed<RealtimeRow[]>(() =>
  realtime.value.reduce<RealtimeRow[]>((acc, s) => {
    for (const f of s.factors) {
      acc.push({
        stationName: s.stationName || s.stationCode,
        factorCode: f.factorCode,
        unit: f.unit,
        value: f.value,
        ts: f.ts,
        devFlag: f.devFlag,
        quality: f.quality,
      });
    }
    return acc;
  }, []),
);

function valueOf(key: string): string {
  const d = data.value;
  if (!d) return '—';
  switch (key) {
    case 'stationTotal':
      return String(d.stationTotal);
    case 'alarmTotal':
      return String(d.alarmTotal);
    case 'frameToday':
      return (d.frameToday ?? 0).toLocaleString();
    case 'dataValidRate':
      if (!d.minuteToday) return '—';
      return `${((d.minuteValidRate ?? 0) * 100).toFixed(1)}%`;
    default:
      return '—';
  }
}

function flagOf(row: RealtimeRow): { text: string; type: 'success' | 'danger' | 'info' } {
  if (row.quality === 1) return { text: 'N', type: 'success' };
  if (row.devFlag) return { text: row.devFlag, type: 'danger' };
  return { text: '未标记', type: 'info' };
}

async function load() {
  loading.value = true;
  try {
    const [overview, rt, alarmPage, logPage, changes] = await Promise.all([
      monitorApi.overview(),
      monitorApi.realtime(300),
      alarmApi.page({ status: 'OPEN', page: 1, pageSize: 5 }),
      messageApi.logs({ page: 1, pageSize: 5 }),
      deviceApi.changes({ limit: 5 }),
    ]);
    data.value = overview;
    realtime.value = rt;
    alarms.value = alarmPage.list;
    msgLogs.value = logPage.list;
    paramChanges.value = changes;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="kpis">
      <div v-for="c in cards" :key="c.key" class="kpi">
        <span class="label">{{ c.label }}</span>
        <strong :style="{ color: c.color }">{{ valueOf(c.key) }}</strong>
        <span class="sub">截至 {{ data?.lastFrameAt ? new Date(data.lastFrameAt).toLocaleString() : '—' }}</span>
      </div>
    </div>

    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-head">
          <span>站房实时值</span>
          <span class="sub">{{ realtime.length }} 个站房 · {{ rows.length }} 项因子</span>
        </div>
      </template>
      <el-table :data="rows" size="small" max-height="340" empty-text="暂无分钟数据，等待数采仪上报">
        <el-table-column prop="stationName" label="站房" min-width="150" show-overflow-tooltip />
        <el-table-column prop="factorCode" label="因子" width="110" />
        <el-table-column label="数值" width="120" align="right">
          <template #default="{ row }">
            {{ row.value == null ? '—' : Number(row.value).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="数据时间" min-width="170">
          <template #default="{ row }">
            {{ row.ts ? new Date(row.ts).toLocaleString() : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="标记" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="flagOf(row).type">{{ flagOf(row).text }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="grid">
      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-head">
            <span>运维计划日历</span>
            <el-tag size="small" type="warning">待接入</el-tag>
          </div>
        </template>
        <p class="muted">
          四色状态：今日计划 / 历史计划 / 未计划 / 下月计划。参考 <code>docs/05</code> 的 W23 页面说明实现。
        </p>
      </el-card>

      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-head">
            <span>报警待办</span>
            <el-tag size="small" type="danger">{{ data?.alarmOpenTotal ?? 0 }}</el-tag>
          </div>
        </template>
        <ul v-if="alarms.length" class="alarm-list">
          <li v-for="a in alarms" :key="a.id">
            <el-tag size="small" :type="a.level === 'P1' ? 'danger' : a.level === 'P2' ? 'warning' : 'info'">
              {{ a.level }}
            </el-tag>
            <span class="alarm-main">{{ a.stationName || a.stationId }} · {{ a.factorCode || a.ruleName }}</span>
            <span class="alarm-time">{{ new Date(a.beginAt).toLocaleTimeString() }}</span>
          </li>
        </ul>
        <p v-else class="muted">暂无未处理报警。</p>
      </el-card>

      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-head">
            <span>消息触达</span>
            <el-tag size="small" type="info">{{ msgLogs.length }} 条</el-tag>
          </div>
        </template>
        <ul v-if="msgLogs.length" class="msg-list">
          <li v-for="m in msgLogs" :key="m.id">
            <el-tag size="small" :type="m.status === 'SENT' ? 'success' : m.status === 'FAILED' ? 'danger' : 'info'">
              {{ m.channel }} · {{ m.status }}
            </el-tag>
            <span class="msg-main">{{ m.content || m.receiver || m.errorMsg }}</span>
            <span class="msg-time">{{ new Date(m.sentAt).toLocaleTimeString() }}</span>
          </li>
        </ul>
        <p v-else class="muted">暂无发送记录。配置飞书 Webhook 后可在「通道」里核对。</p>
      </el-card>

      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-head">
            <span>工作参数变更</span>
            <el-tag size="small" type="warning">{{ paramChanges.length }} 条</el-tag>
          </div>
        </template>
        <ul v-if="paramChanges.length" class="alarm-list">
          <li v-for="c in paramChanges" :key="c.id">
            <el-tag size="small" type="warning">变更</el-tag>
            <span class="alarm-main">站房 {{ c.stationId }} · {{ c.paramCode }} {{ c.oldValue }} → {{ c.newValue }}</span>
            <span class="alarm-time">{{ new Date(c.changeTime).toLocaleTimeString() }}</span>
          </li>
        </ul>
        <p v-else class="muted">近期无工作参数变更。</p>
      </el-card>

      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-head"><span>站房状态分布</span></div>
        </template>
        <ul class="status-list">
          <li v-for="(v, k) in data?.stationByStatus ?? {}" :key="k">
            <span class="status-name">{{ k }}</span>
            <span class="status-val">{{ v }}</span>
          </li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.kpi {
  background: #fff;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.label {
  font-size: 12.5px;
  color: #6b7788;
}
.kpi strong {
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.02em;
}
.sub {
  font-size: 11.5px;
  color: #9aa6b6;
}
.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.card {
  border-radius: 10px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 14px;
}
.muted {
  color: #6b7788;
  font-size: 13px;
  line-height: 1.8;
  margin: 0;
}
.alarm-list,
.msg-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.alarm-list li,
.msg-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px dashed #eef2f7;
  font-size: 13px;
  color: #35435a;
}
.alarm-main,
.msg-main {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.alarm-time,
.msg-time {
  font-size: 11.5px;
  color: #9aa6b6;
}
.status-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.status-list li {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
  border-bottom: 1px dashed #eef2f7;
  font-size: 13px;
  color: #35435a;
}
@media (max-width: 1100px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
