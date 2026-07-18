<template>
  <div class="home-page">
    <section class="mall-page hero-section">
      <img src="/mall/hero-fruit-basket.png" alt="新鲜水果篮" />
      <div class="hero-copy">
        <h1>把今天的新鲜，送到家</h1>
        <p>严选当季水果，库存与订单实时同步</p>
        <el-button class="mall-primary-button hero-button" type="primary" @click="scrollToProducts">选购鲜果</el-button>
      </div>
    </section>

    <section ref="productsSection" class="mall-page product-section">
      <div class="section-heading">
        <div>
          <h2>{{ keyword ? `“${keyword}”的搜索结果` : '本周鲜果' }}</h2>
          <p>每一份鲜果都由商城库存实时保障</p>
        </div>
        <div class="mobile-search">
          <el-input v-model="mobileKeyword" :prefix-icon="Search" placeholder="搜索鲜果" clearable @keyup.enter="applyMobileSearch" />
        </div>
      </div>
      <div class="category-rail" aria-label="商品分类">
        <button type="button" :class="{ active: !category }" @click="selectCategory('')">全部</button>
        <button v-for="item in categories" :key="item" type="button" :class="{ active: category === item }" @click="selectCategory(item)">{{ item }}</button>
      </div>

      <div v-if="loadError" class="load-error" role="status">
        <span>商品服务暂时不可用，请确认后端与数据库已启动。</span>
        <el-button link type="primary" @click="fetchProducts">重新加载</el-button>
      </div>

      <div v-loading="loading" class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>
      <el-empty v-if="!loading && products.length === 0" description="没有找到符合条件的鲜果" />
      <div v-if="total > pageSize" class="pagination-row">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize" :current-page="pageNum" @current-change="changePage" />
      </div>
    </section>

    <section class="mall-page service-strip">
      <div><el-icon><CircleCheck /></el-icon><span><strong>严选品质</strong><small>产地直采，严格品控</small></span></div>
      <div><el-icon><Van /></el-icon><span><strong>新鲜速达</strong><small>当日采摘，次日送达</small></span></div>
      <div><el-icon><Box /></el-icon><span><strong>库存同步</strong><small>实时库存，安心选购</small></span></div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Box, CircleCheck, Search, Van } from '@element-plus/icons-vue'
import ProductCard from '@/components/mall/ProductCard.vue'
import { getMallCategories, getMallProducts } from '@/api/mall'

const route = useRoute()
const router = useRouter()
const productsSection = ref()
const products = ref([])
const categories = ref([])
const category = ref('')
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)
const loadError = ref(false)
const mobileKeyword = ref('')
const keyword = computed(() => String(route.query.keyword || '').trim())

const fetchProducts = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await getMallProducts({ pageNum: pageNum.value, pageSize, keyword: keyword.value || undefined, category: category.value || undefined })
    products.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } catch {
    products.value = []
    total.value = 0
    loadError.value = true
  } finally { loading.value = false }
}

const selectCategory = value => { category.value = value; pageNum.value = 1; fetchProducts() }
const changePage = value => { pageNum.value = value; fetchProducts(); scrollToProducts() }
const scrollToProducts = () => productsSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
const applyMobileSearch = () => router.push({ path: '/mall', query: mobileKeyword.value.trim() ? { keyword: mobileKeyword.value.trim() } : {} })

watch(keyword, value => { mobileKeyword.value = value; pageNum.value = 1; fetchProducts() })
watch(() => route.query.browse, async value => { if (value) { await nextTick(); scrollToProducts() } })
onMounted(async () => {
  mobileKeyword.value = keyword.value
  const [categoryResult] = await Promise.allSettled([getMallCategories(), fetchProducts()])
  categories.value = categoryResult.status === 'fulfilled' ? (categoryResult.value.data || []) : []
  if (route.query.browse) await nextTick().then(scrollToProducts)
})
</script>

<style scoped>
.home-page { padding-top: 18px; }
.hero-section { position: relative; min-height: 355px; border-radius: 22px; overflow: hidden; background: var(--mall-mint); }
.hero-section > img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.hero-copy { position: relative; z-index: 1; max-width: 610px; padding: 82px 46px; }
.hero-copy h1 { margin: 0; font-size: clamp(36px, 4vw, 58px); line-height: 1.12; letter-spacing: -2px; }
.hero-copy p { margin: 22px 0 28px; color: #59635d; font-size: 18px; }
.hero-button { min-width: 146px; }
.product-section { padding-top: 42px; scroll-margin-top: 82px; }
.section-heading { display: flex; justify-content: space-between; gap: 24px; align-items: flex-end; }
.section-heading h2 { margin: 0; font-size: 30px; }
.section-heading p { margin: 8px 0 0; color: var(--mall-muted); }
.mobile-search { display: none; width: 260px; }
.category-rail { display: flex; gap: 10px; margin: 24px 0 20px; overflow-x: auto; padding-bottom: 4px; }
.category-rail { scrollbar-width: none; }
.category-rail::-webkit-scrollbar { display: none; }
.category-rail button { flex: 0 0 auto; border: 1px solid var(--mall-border); background: #fff; color: #4b534e; border-radius: 10px; padding: 9px 22px; cursor: pointer; transition: .2s; }
.category-rail button:hover, .category-rail button.active { color: #fff; background: var(--mall-green); border-color: var(--mall-green); }
.load-error { margin-bottom: 18px; padding: 14px 18px; border: 1px solid #f0d8b6; border-radius: 12px; background: #fff9ef; color: #8a5b20; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.product-grid { min-height: 320px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; }
.pagination-row { display: flex; justify-content: center; margin-top: 30px; }
.service-strip { margin-top: 28px; min-height: 110px; border: 1px solid #dcebe2; border-radius: 18px; background: linear-gradient(90deg, #fbfefc, #f1faf4); display: grid; grid-template-columns: repeat(3, 1fr); align-items: center; }
.service-strip > div { padding: 10px 42px; display: flex; align-items: center; gap: 16px; border-right: 1px solid #dcebe2; }
.service-strip > div:last-child { border: 0; }
.service-strip .el-icon { color: var(--mall-green); font-size: 36px; }
.service-strip span { display: flex; flex-direction: column; gap: 6px; }
.service-strip small { color: var(--mall-muted); }
@media (max-width: 1020px) { .product-grid { grid-template-columns: repeat(3, 1fr); } .hero-copy { padding: 64px 36px; max-width: 52%; } .service-strip > div { padding: 10px 20px; } }
@media (max-width: 760px) { .home-page { padding-top: 12px; } .hero-section { min-height: 470px; } .hero-section > img { object-position: 68% bottom; } .hero-copy { max-width: none; padding: 38px 24px; background: linear-gradient(180deg, rgba(242,251,246,.98) 0%, rgba(242,251,246,.93) 46%, rgba(242,251,246,0) 76%); } .hero-copy h1 { font-size: 36px; letter-spacing: -1px; max-width: 310px; } .hero-copy p { max-width: 280px; font-size: 15px; } .mobile-search { display: block; width: 100%; } .section-heading { align-items: stretch; flex-direction: column; } .product-grid { grid-template-columns: repeat(2, minmax(0,1fr)); gap: 12px; } .service-strip { grid-template-columns: 1fr; } .service-strip > div { border-right: 0; border-bottom: 1px solid #dcebe2; padding: 20px 28px; } }
@media (max-width: 430px) { .product-grid { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .hero-section { min-height: 410px; } }
</style>
