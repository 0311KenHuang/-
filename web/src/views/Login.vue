<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ username: '', password: '', tenantCode: '' });

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

async function submit() {
  const ok = await formRef.value?.validate().catch(() => false);
  if (!ok) return;
  loading.value = true;
  try {
    await auth.login({
      username: form.username,
      password: form.password,
      tenantCode: form.tenantCode || undefined,
    });
    ElMessage.success('登录成功');
    router.replace((route.query.redirect as string) || '/dashboard');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="panel">
      <div class="intro">
        <h1>污染源自动监控设施<br />第三方运维信息管理系统</h1>
        <p>数据接入 · 运维闭环 · 监管取证</p>
        <ul>
          <li>HJ 212—2025 全量报文接入</li>
          <li>运维证据链：计划 → 任务 → 到场 → 记录 → 审核</li>
          <li>飞书 / 小程序 / 短信 多端触达</li>
        </ul>
      </div>

      <div class="form-wrap">
        <h2>账号登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.tenantCode" placeholder="租户编码（单租户可留空）" />
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>
        <p class="hint">演示账号见 README（admin / Admin@123）</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: radial-gradient(1200px 600px at 20% 10%, #1b3a6b 0%, #0d1a2c 55%, #08111d 100%);
}
.panel {
  width: min(920px, 92vw);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(4, 12, 24, 0.45);
}
.intro {
  padding: 44px 40px;
  color: #e8f0ff;
  background: linear-gradient(160deg, #14305a 0%, #0d1f38 100%);
}
.intro h1 {
  font-size: 23px;
  line-height: 1.5;
  margin: 0 0 12px;
  letter-spacing: 0.01em;
}
.intro p {
  margin: 0 0 26px;
  color: #9db6da;
  font-size: 13px;
  letter-spacing: 0.08em;
}
.intro ul {
  margin: 0;
  padding-left: 18px;
  color: #b9cbe4;
  font-size: 13px;
  line-height: 2;
}
.form-wrap {
  padding: 44px 36px;
}
.form-wrap h2 {
  margin: 0 0 22px;
  font-size: 18px;
  color: #16202e;
}
.submit {
  width: 100%;
}
.hint {
  margin-top: 16px;
  font-size: 12px;
  color: #8a97a8;
  text-align: center;
}
@media (max-width: 760px) {
  .panel {
    grid-template-columns: 1fr;
  }
  .intro {
    display: none;
  }
}
</style>
