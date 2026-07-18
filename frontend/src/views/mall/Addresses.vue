<template>
  <div class="mall-page address-page">
    <header class="page-head">
      <div><h1>收货地址</h1><p>管理结算时可使用的联系人与配送地址，最多保存 20 个。</p></div>
      <el-button class="mall-primary-button" type="primary" :icon="Plus" @click="openEditor()">新增地址</el-button>
    </header>

    <section v-loading="loading" class="address-grid">
      <article v-for="address in addresses" :key="address.id" class="address-card">
        <div class="card-top">
          <span class="location-mark"><el-icon><Location /></el-icon></span>
          <el-tag v-if="address.isDefault === 1" type="success" effect="light">默认地址</el-tag>
        </div>
        <h2>{{ address.recipientName }} <small>{{ maskPhone(address.phone) }}</small></h2>
        <p>{{ fullAddress(address) }}</p>
        <footer>
          <el-button v-if="address.isDefault !== 1" link type="success" @click="makeDefault(address)">设为默认</el-button>
          <span v-else>结算优先使用</span>
          <div><el-button link @click="openEditor(address)">编辑</el-button><el-button link type="danger" @click="remove(address)">删除</el-button></div>
        </footer>
      </article>
      <button v-if="addresses.length < 20" class="address-card add-card" type="button" @click="openEditor()">
        <el-icon><Plus /></el-icon><strong>添加新的收货地址</strong><span>用于商城下单与配送</span>
      </button>
    </section>
    <el-empty v-if="!loading && !addresses.length" description="还没有收货地址" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑收货地址' : '新增收货地址'" width="min(580px, 94vw)" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid"><el-form-item label="收货人" prop="recipientName"><el-input v-model="form.recipientName" maxlength="50" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" maxlength="11" /></el-form-item></div>
        <div class="form-grid three"><el-form-item label="省" prop="province"><el-input v-model="form.province" /></el-form-item><el-form-item label="市" prop="city"><el-input v-model="form.city" /></el-form-item><el-form-item label="区 / 县"><el-input v-model="form.district" /></el-form-item></div>
        <el-form-item label="详细地址" prop="detailAddress"><el-input v-model="form.detailAddress" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item>
        <el-checkbox v-model="form.defaultChecked">设为默认地址</el-checkbox>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button class="mall-primary-button" type="primary" :loading="saving" @click="save">保存地址</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Plus } from '@element-plus/icons-vue'
import { createAddress, deleteAddress, getAddresses, setDefaultAddress, updateAddress } from '@/api/mall'

const loading = ref(false); const saving = ref(false); const addresses = ref([]); const dialogVisible = ref(false); const formRef = ref()
const blank = () => ({ id: null, recipientName: '', phone: '', province: '', city: '', district: '', detailAddress: '', defaultChecked: false })
const form = reactive(blank())
const rules = {
  recipientName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }], city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}
const fullAddress = value => `${value.province || ''}${value.city || ''}${value.district || ''}${value.detailAddress || ''}`
const maskPhone = value => value?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') || '--'
const load = async () => { loading.value = true; try { const res = await getAddresses(); addresses.value = res.data || [] } finally { loading.value = false } }
const openEditor = address => { Object.assign(form, address ? { ...address, defaultChecked: address.isDefault === 1 } : blank()); dialogVisible.value = true }
const save = async () => { if (!await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { const payload = { ...form, isDefault: form.defaultChecked ? 1 : 0 }; form.id ? await updateAddress(form.id, payload) : await createAddress(payload); dialogVisible.value = false; await load(); ElMessage.success('地址已保存') } finally { saving.value = false } }
const makeDefault = async address => { await setDefaultAddress(address.id); await load(); ElMessage.success('默认地址已更新') }
const remove = async address => { try { await ElMessageBox.confirm('删除后不可用于新的订单，确认继续吗？', '删除地址', { type: 'warning' }); await deleteAddress(address.id); await load(); ElMessage.success('地址已删除') } catch (error) { if (error !== 'cancel' && error !== 'close') throw error } }
onMounted(load)
</script>

<style scoped>
.address-page { padding-top: 42px; }.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; margin-bottom: 28px; }.page-head h1 { margin: 0; font-size: 36px; }.page-head p { margin: 9px 0 0; color: var(--mall-muted); }.address-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; min-height: 180px; }.address-card { min-height: 210px; padding: 24px; border: 1px solid var(--mall-border); border-radius: 18px; background: #fff; box-sizing: border-box; display: flex; flex-direction: column; box-shadow: 0 10px 30px rgba(31, 69, 44, .05); }.card-top { display: flex; justify-content: space-between; }.location-mark { width: 42px; height: 42px; display: grid; place-items: center; color: var(--mall-green); background: var(--mall-mint); border-radius: 12px; font-size: 22px; }.address-card h2 { margin: 20px 0 10px; font-size: 19px; }.address-card h2 small { color: var(--mall-muted); font-size: 13px; font-weight: 500; }.address-card p { color: #57615b; line-height: 1.7; margin: 0; flex: 1; }.address-card footer { margin-top: 22px; padding-top: 16px; border-top: 1px solid var(--mall-border); display: flex; justify-content: space-between; align-items: center; color: var(--mall-muted); font-size: 13px; }.add-card { align-items: center; justify-content: center; gap: 10px; color: var(--mall-green); border-style: dashed; cursor: pointer; }.add-card .el-icon { font-size: 32px; }.add-card span { color: var(--mall-muted); }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }.form-grid.three { grid-template-columns: repeat(3, 1fr); }
@media (max-width: 960px) { .address-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .page-head { align-items: stretch; flex-direction: column; }.page-head h1 { font-size: 30px; }.address-grid { grid-template-columns: 1fr; }.form-grid, .form-grid.three { grid-template-columns: 1fr; gap: 0; } }
</style>
