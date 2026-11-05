<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  opsApi,
  stationApi,
  type OpsCoverage,
  type OpsPlan,
  type OpsRecord,
  type OpsTaskView,
} from '@/api';

const loading = ref(false);
const tasks = ref<OpsTaskView[]>([]);
const plans = ref<OpsPlan[]>([]);
const records = ref<OpsRecord[]>([]);
const coverage = ref<OpsCoverage | null>(null);
const status = ref('');
const period = ref(new Date().toISOString().slice(0, 7));
const traceResult = ref<{ fakeMarks: string[]; missingMarks: string[] } | null>(null);
const recordDialog = ref(false);
const recordTask = ref<OpsTaskView | null>(null);
const conclusion = ref('现场作业完成，设备运行正常');
const saving = ref(false);

const statusType = (s: string) =>
  s === 'CLOSED' ? 'success' : s === 'RETURNED' ? 'danger' : s === 'SUBMITTED' ? 'warning' : 'info';

async function load() {
  loading.value = true;
  try {
    const [taskPage, planList, cov, pending] = await Promise.all([
      opsApi.tasks({ page: 1, pageSize: 50, status: status.value || undefined }),
      opsApi.plans(period.value),
      opsApi.coverage(period.value),
      opsApi.pendingRecords(),
    ]);
    tasks.value = taskPage.list;
    plans.value = planList;
    coverage.value = cov;
    records.value = pending;
  } finally {
    loading.value = false;
  }
}

async function act(task: OpsTaskView, action: string) {
  const data: Record<string, unknown> = { action };
  if (action === 'ARRIVE') {
    // 实际现场由小程序上报 GPS；管理端演示用站房坐标，服务端仍按 300 米围栏校验
    const station = await stationApi.detail(task.stationId);
    if (!station.lng || !station.lat) {
      ElMessage.error('站房未配置坐标，无法完成到场打卡');
      return;
    }
    data.lng = Number(station.lng);
    data.lat = Number(station.lat);
    data.remark = '到场打卡';
  }
  await opsApi.act(task.id, data);
  ElMessage.success(`已${action === 'ACCEPT' ? '接单' : action === 'ARRIVE' ? '到场' : '提交'}`);
  await load();
}

async function generate() {
  const r = await opsApi.generate(period.value, ['巡检', '校准', '试剂更换']);
  ElMessage.success(`生成计划 ${r.plans} 份 / 明细 ${r.items} 条`);
  await load();
}

async function publish(plan: OpsPlan) {
  const r = await opsApi.publish(plan.id);
  ElMessage.success(`下发任务 ${r.tasks} 张`);
  await load();
}

async function review(record: OpsRecord, result: string) {
  let reason = '';
  if (result === 'REJECT') {
    const input = await ElMessageBox.prompt('请填写驳回原因', '驳回记录', {
      inputValidator: (v: string) => (v && v.trim() ? true : '原因不能为空'),
    });
    reason = input.value;
  }
  await opsApi.review(record.id, result, reason);
  ElMessage.success(result === 'PASS' ? '审核通过，任务与关联报警已闭环' : '已驳回');
  await load();
}

async function checkTrace() {
  traceResult.value = await opsApi.trace(period.value + '-01', period.value + '-28');
  ElMessage.success('标记核验完成');
}

function openRecord(task: OpsTaskView) {
  recordTask.value = task;
  conclusion.value = '现场作业完成，设备运行正常';
  recordDialog.value = true;
}

async function saveRecord() {
  if (!recordTask.value) return;
  saving.value = true;
  try {
    const record = await opsApi.submitRecord({
      taskId: recordTask.value.id,
      conclusion: conclusion.value,
      items: [
        {
          itemCode: 'CHECK',
          itemName: '现场检查',
          valueType: 'TEXT',
          value: conclusion.value,
          normalFlag: 1,
        },
      ],
    });
    await opsApi.submitForAudit(record.id);
    ElMessage.success('记录已提交审核');
    recordDialog.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div v-loading="loading" class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>运维计划 · {{ period }}</span>
          <div class="ops">
            <el-date-picker v-model="period" type="month" value-format="YYYY-MM" size="small" @change="load" />
            <el-button size="small" type="primary" @click="generate">生成月度计划</el-button>
          </div>
        </div>
      </template>
      <p class="muted">
        覆盖率 {{ ((coverage?.coverageRate ?? 0) * 100).toFixed(1) }}%
        （{{ coverage?.plannedStations ?? 0 }} / {{ coverage?.totalStations ?? 0 }} 站房）
        <span v-if="coverage?.unplannedStationIds?.length">
          · 未排期站房 {{ coverage.unplannedStationIds.join('、') }}
        </span>
      </p>
      <el-table :data="plans" size="small" empty-text="本月暂无计划">
        <el-table-column prop="planNo" label="计划号" min-width="180" />
        <el-table-column prop="stationId" label="站房" width="90" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'DONE'" size="small" type="primary" link @click="publish(row)">
              下发任务
            </el-button>
            <span v-else class="muted">已下发</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>任务执行</span>
          <div class="ops">
            <el-select v-model="status" size="small" clearable placeholder="全部状态" style="width: 140px" @change="load">
              <el-option label="待接单" value="DISPATCHED" />
              <el-option label="已接单" value="ACCEPTED" />
              <el-option label="已到场" value="ARRIVED" />
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="tasks" size="small" empty-text="暂无任务，先在上方生成并下发计划">
        <el-table-column prop="taskNo" label="任务号" min-width="180" />
        <el-table-column prop="stationName" label="站房" min-width="140" />
        <el-table-column prop="opsType" label="作业类型" width="110" />
        <el-table-column prop="assigneeName" label="责任人" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超期" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isOverdue" size="small" type="danger">超期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DISPATCHED'" size="small" link type="primary" @click="act(row, 'ACCEPT')">接单</el-button>
            <el-button v-if="row.status === 'ACCEPTED'" size="small" link type="primary" @click="act(row, 'ARRIVE')">到场打卡</el-button>
            <el-button v-if="row.status === 'ARRIVED'" size="small" link type="primary" @click="openRecord(row)">填报记录</el-button>
            <span v-if="row.status === 'CLOSED'" class="muted">已完成</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>待审核记录</span>
          <el-button size="small" @click="checkTrace">标记 ⇄ 工单核验</el-button>
        </div>
      </template>
      <el-table :data="records" size="small" empty-text="暂无待审核记录">
        <el-table-column prop="recordNo" label="记录号" min-width="180" />
        <el-table-column prop="opsType" label="作业类型" width="120" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" link type="success" @click="review(row, 'PASS')">通过</el-button>
            <el-button size="small" link type="danger" @click="review(row, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="traceResult" class="trace">
        <p class="muted">疑似虚假标记 {{ traceResult.fakeMarks.length }} 条 / 漏标 {{ traceResult.missingMarks.length }} 条</p>
        <ul v-if="traceResult.fakeMarks.length || traceResult.missingMarks.length">
          <li v-for="t in [...traceResult.fakeMarks, ...traceResult.missingMarks]" :key="t">{{ t }}</li>
        </ul>
      </div>
    </el-card>

    <el-dialog v-model="recordDialog" title="填报运维记录" width="480px">
      <p class="muted">任务 {{ recordTask?.taskNo }} · {{ recordTask?.stationName }}</p>
      <el-input v-model="conclusion" type="textarea" :rows="3" placeholder="作业结论" />
      <template #footer>
        <el-button @click="recordDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRecord">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 14px;
}
.ops {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 400;
}
.muted {
  color: #6b7788;
  font-size: 13px;
}
.trace {
  margin-top: 12px;
  border-top: 1px dashed #e8edf4;
  padding-top: 10px;
}
.trace ul {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: #b91c1c;
}
</style>
