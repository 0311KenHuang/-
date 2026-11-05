<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { stationApi, type Station } from '@/api';

const STATION_TYPES = [
  { value: 'WASTE_WATER', label: '废水' },
  { value: 'WASTE_GAS', label: '废气' },
  { value: 'VOC', label: 'VOC' },
  { value: 'INCINERATION', label: '垃圾焚烧' },
  { value: 'AIR', label: '空气站' },
  { value: 'GROUNDWATER', label: '地下水' },
];

const STATUS_MAP: Record<string, { text: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  NORMAL: { text: '正常', type: 'success' },
  ABNORMAL: { text: '异常', type: 'warning' },
  STOPPED: { text: '停运', type: 'info' },
  MAINTAINING: { text: '维护', type: 'danger' },
};

const loading = ref(false);
const list = ref<Station[]>([]);
const total = ref(0);
const query = reactive({
  page: 1,
  pageSize: 20,
  keyword: '',
  stationType: '',
  bizStatus: '',
});

async function load() {
  loading.value = true;
  try {
    const res = await stationApi.page({ ...query });
    list.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.keyword = '';
  query.stationType = '';
  query.bizStatus = '';
  query.page = 1;
  void load();
}

async function changeStatus(row: Station, status: string) {
  const res = await stationApi.changeStatus(row.id, status, status === 'STOPPED' ? '手工停运' : undefined);
  Object.assign(row, res);
  ElMessage.success('状态已更新');
}

onMounted(load);
</script>

<template>
  <el-card shadow="never" class="wrap">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="站房名称 / 编码"
        clearable
        style="width: 220px"
        @keyup.enter="((query.page = 1), load())"
      />
      <el-select v-model="query.stationType" placeholder="站房类型" clearable style="width: 140px">
        <el-option v-for="t in STATION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="query.bizStatus" placeholder="运行状态" clearable style="width: 130px">
        <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.text" :value="k" />
      </el-select>
      <el-button type="primary" @click="((query.page = 1), load())">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer" />
      <el-button type="primary" plain>新增站房</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe height="calc(100vh - 250px)">
      <el-table-column prop="stationCode" label="站房编码" width="140" fixed />
      <el-table-column prop="name" label="站房名称" min-width="180" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          {{ STATION_TYPES.find((t) => t.value === row.stationType)?.label ?? row.stationType }}
        </template>
      </el-table-column>
      <el-table-column label="运行状态" width="110">
        <template #default="{ row }">
          <el-tag :type="STATUS_MAP[row.bizStatus]?.type ?? 'info'" size="small" effect="light">
            {{ STATUS_MAP[row.bizStatus]?.text ?? row.bizStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enterpriseId" label="企业 ID" width="100" />
      <el-table-column prop="opsEngineerId" label="责任工程师" width="120">
        <template #default="{ row }">{{ row.opsEngineerId ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary">详情</el-button>
          <el-button link type="warning" @click="changeStatus(row, 'MAINTAINING')">维护</el-button>
          <el-button link type="info" @click="changeStatus(row, 'STOPPED')">停运</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="((query.page = 1), load())"
      />
    </div>
  </el-card>
</template>

<style scoped>
.wrap {
  border-radius: 10px;
}
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.spacer {
  flex: 1;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
