<template>
  <div class="circle-plaza">
    <!-- 蜂巢装饰背景 -->
    <div class="hex-bg" />

    <!-- 顶部搜索栏 -->
    <header class="plaza-header">
      <h1 class="plaza-title">
        <span class="title-icon">⬡</span>
        圈子广场
      </h1>
      <div class="search-bar">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="搜索圈子名称..."
          @keyup.enter="handleSearch"
        />
        <button v-if="keyword" class="search-clear" @click="clearSearch">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
    </header>

    <!-- 分类标签横向滚动 -->
    <nav class="category-scroll">
      <div class="category-track">
        <button
          v-for="cat in categoryList"
          :key="cat"
          :class="['category-tag', { active: activeCategory === cat }]"
          @click="switchCategory(cat)"
        >
          {{ cat }}
        </button>
      </div>
    </nav>

    <!-- 推荐圈子Banner轮播 -->
    <section v-if="recommendList.length" class="banner-section">
      <div class="banner-carousel">
        <div
          v-for="(circle, idx) in recommendList"
          :key="circle.id"
          :class="['banner-slide', { active: currentBanner === idx }]"
          @click="goDetail(circle.id)"
        >
          <img :src="circle.banner" :alt="circle.name" class="banner-img" />
          <div class="banner-overlay" />
          <div class="banner-info">
            <div class="banner-hex-avatar">
              <img :src="circle.avatar" :alt="circle.name" />
            </div>
            <div class="banner-text">
              <h3 class="banner-name">{{ circle.name }}</h3>
              <p class="banner-intro">{{ circle.intro }}</p>
            </div>
          </div>
        </div>
      </div>
      <!-- 轮播指示器 -->
      <div class="banner-dots">
        <span
          v-for="(_, idx) in recommendList"
          :key="idx"
          :class="['banner-dot', { active: currentBanner === idx }]"
          @click="currentBanner = idx"
        />
      </div>
    </section>

    <!-- 圈子卡片网格 -->
    <section class="circle-grid-section">
      <div class="section-head">
        <h2 class="section-title">
          <span class="hex-dot" />
          {{ activeCategory === '全部' ? '全部圈子' : activeCategory }}
        </h2>
        <span class="section-count">共 {{ totalCircles }} 个</span>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading && !circleList.length" />

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!loading && !circleList.length"
        description="暂无圈子，快来创建一个吧~"
      />

      <!-- 卡片网格 -->
      <div v-else class="circle-grid">
        <CircleCard
          v-for="circle in circleList"
          :key="circle.id"
          :circle="circle"
          @click="goDetail(circle.id)"
        />
      </div>

      <!-- 上拉加载更多 -->
      <div v-if="circleList.length" class="load-more-area">
        <LoadingSpinner v-if="loadingMore" size="small" />
        <p v-else-if="!hasMore" class="no-more">— 没有更多了 —</p>
      </div>
    </section>

    <!-- 创建圈子入口按钮 -->
    <button class="create-fab" @click="goCreate" title="创建圈子">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="28" height="28">
        <path d="M12 5v14M5 12h14" />
      </svg>
    </button>

    <!-- 下拉刷新遮罩 -->
    <transition name="refresh-fade">
      <div v-if="refreshing" class="refresh-overlay">
        <div class="refresh-hex">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
          </svg>
        </div>
        <span>刷新中...</span>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
/**
 * 圈子广场页面
 * 提供圈子搜索、分类筛选、推荐轮播、圈子列表浏览及创建入口
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCircles, getCategories, getRecommendCircles } from '@/api/circle'
import type { Circle } from '@/types'
import CircleCard from '@/components/CircleCard.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

// ---------- 响应式状态 ----------
/** 搜索关键词 */
const keyword = ref('')
/** 当前选中分类 */
const activeCategory = ref('全部')
/** 分类列表 */
const categoryList = ref<string[]>(['全部'])
/** 推荐圈子列表 */
const recommendList = ref<Circle[]>([])
/** 圈子列表 */
const circleList = ref<Circle[]>([])
/** 当前页码 */
const page = ref(1)
/** 每页数量 */
const pageSize = 12
/** 总圈子数 */
const totalCircles = ref(0)
/** 是否有更多数据 */
const hasMore = ref(true)
/** 首次加载中 */
const loading = ref(false)
/** 加载更多中 */
const loadingMore = ref(false)
/** 下拉刷新中 */
const refreshing = ref(false)
/** 当前轮播索引 */
const currentBanner = ref(0)

/** 轮播定时器 */
let bannerTimer: ReturnType<typeof setInterval> | null = null

// ---------- 计算属性 ----------
/** 总圈子数展示 */
const totalDisplay = computed(() => totalCircles.value)

// ---------- 方法 ----------

/**
 * 加载圈子分类列表
 * @returns {Promise<void>}
 */
async function loadCategories(): Promise<void> {
  try {
    const res = await getCategories()
    categoryList.value = ['全部', ...(res.data || [])]
  } catch {
    categoryList.value = ['全部', '游戏', '影视', '户外', '读书', '摄影', '二次元', '音乐', '美食', '旅行']
  }
}

/**
 * 加载推荐圈子
 * @returns {Promise<void>}
 */
async function loadRecommend(): Promise<void> {
  try {
    const res = await getRecommendCircles()
    recommendList.value = res.data || []
    if (recommendList.value.length) {
      startBannerAutoPlay()
    }
  } catch {
    recommendList.value = []
  }
}

/**
 * 加载圈子列表
 * @param {boolean} append - 是否追加模式（加载更多）
 * @returns {Promise<void>}
 */
async function loadCircles(append = false): Promise<void> {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const params: { keyword?: string; category?: string; page: number; pageSize: number } = {
      page: page.value,
      pageSize
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (activeCategory.value !== '全部') params.category = activeCategory.value

    const res = await getCircles(params)
    const data = res.data
    if (append) {
      circleList.value.push(...data.list)
    } else {
      circleList.value = data.list
    }
    totalCircles.value = data.total
    hasMore.value = data.hasMore
  } catch {
    if (!append) circleList.value = []
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/**
 * 切换分类标签
 * @param {string} cat - 分类名称
 */
function switchCategory(cat: string): void {
  if (activeCategory.value === cat) return
  activeCategory.value = cat
  page.value = 1
  hasMore.value = true
  loadCircles()
}

/**
 * 搜索圈子
 */
function handleSearch(): void {
  page.value = 1
  hasMore.value = true
  loadCircles()
}

/**
 * 清空搜索关键词并刷新
 */
function clearSearch(): void {
  keyword.value = ''
  page.value = 1
  hasMore.value = true
  loadCircles()
}

/**
 * 下拉刷新
 * @returns {Promise<void>}
 */
async function onRefresh(): Promise<void> {
  refreshing.value = true
  page.value = 1
  hasMore.value = true
  try {
    await Promise.all([loadCircles(), loadRecommend()])
  } finally {
    refreshing.value = false
  }
}

/**
 * 上拉加载更多
 * @returns {Promise<void>}
 */
async function onLoadMore(): Promise<void> {
  if (loadingMore.value || !hasMore.value) return
  page.value++
  await loadCircles(true)
}

/**
 * 跳转圈子详情
 * @param {number} id - 圈子ID
 */
function goDetail(id: number): void {
  router.push(`/circle/${id}`)
}

/**
 * 跳转创建圈子
 */
function goCreate(): void {
  router.push('/circle/create')
}

/**
 * 启动Banner自动轮播
 */
function startBannerAutoPlay(): void {
  stopBannerAutoPlay()
  bannerTimer = setInterval(() => {
    if (recommendList.value.length <= 1) return
    currentBanner.value = (currentBanner.value + 1) % recommendList.value.length
  }, 4000)
}

/**
 * 停止Banner自动轮播
 */
function stopBannerAutoPlay(): void {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}

/**
 * 处理页面滚动事件，实现上拉加载更多
 * @param {Event} e - 滚动事件
 * @description 仅处理窗口级垂直滚动（忽略分类标签等子元素的横向/局部滚动）
 */
function handleScroll(e: Event): void {
  // 忽略非 window 目标的滚动事件（如分类标签的横向滚动、输入框内部滚动等）
  if (e.target !== document && e.target !== document.documentElement && e.target !== document.body) return

  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const docHeight = document.documentElement.scrollHeight

  // 上拉加载检测
  if (scrollTop + windowHeight >= docHeight - 100 && hasMore.value && !loadingMore.value && !loading.value) {
    onLoadMore()
  }
}

// ---------- 生命周期 ----------
onMounted(() => {
  loadCategories()
  loadRecommend()
  loadCircles()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  stopBannerAutoPlay()
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

// ---------- 页面容器 ----------
.circle-plaza {
  min-height: 100vh;
  background: $ink-50;
  position: relative;
  padding-bottom: 80px;
}

// ---------- 顶部搜索栏 ----------
.plaza-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: $space-4 $space-4 $space-3;
  @include glass(20px, rgba(245, 247, 251, 0.92));
  border-bottom: 1px solid $ink-100;
}

.plaza-title {
  font-family: $font-heading;
  font-size: 22px;
  font-weight: 700;
  color: $ink-900;
  margin-bottom: $space-3;
  display: flex;
  align-items: center;
  gap: $space-2;

  .title-icon {
    color: $mint-500;
    font-size: 20px;
    animation: hex-breathe 3s ease-in-out infinite;
  }
}

.search-bar {
  position: relative;
  display: flex;
  align-items: center;
  background: $ink-50;
  border: 1.5px solid $ink-100;
  border-radius: $radius-pill;
  padding: 0 $space-4;
  transition: border-color $dur-fast $ease-out, box-shadow $dur-fast $ease-out;

  &:focus-within {
    border-color: $mint-500;
    box-shadow: 0 0 0 3px rgba(78, 205, 196, 0.15);
  }
}

.search-icon {
  width: 18px;
  height: 18px;
  color: $ink-300;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  height: 40px;
  padding: 0 $space-3;
  font-size: 14px;
  color: $ink-700;
  background: transparent;

  &::placeholder {
    color: $ink-300;
  }
}

.search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: $ink-300;
  transition: all $dur-fast $ease-out;

  &:hover {
    background: $ink-100;
    color: $ink-500;
  }
}

// ---------- 分类标签 ----------
.category-scroll {
  padding: $space-3 0;
  overflow: hidden;

  .category-track {
    display: flex;
    gap: $space-2;
    padding: 0 $space-4;
    overflow-x: auto;
    @include custom-scrollbar(4px);
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

.category-tag {
  flex-shrink: 0;
  padding: $space-1 $space-4;
  border-radius: $radius-pill;
  font-size: 13px;
  font-weight: 500;
  color: $ink-500;
  background: $ink-50;
  border: 1.5px solid $ink-100;
  transition: all $dur-fast $ease-out;
  white-space: nowrap;

  &:hover {
    color: $mint-600;
    border-color: $mint-300;
  }

  &.active {
    color: #fff;
    background: $grad-mint;
    border-color: transparent;
    box-shadow: $shadow-glow-mint;
  }
}

// ---------- Banner轮播 ----------
.banner-section {
  padding: $space-2 $space-4;
}

.banner-carousel {
  position: relative;
  border-radius: $radius-lg;
  overflow: hidden;
  aspect-ratio: 16 / 7;
  background: $ink-800;
}

.banner-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity $dur-slow $ease-out;
  cursor: pointer;

  &.active {
    opacity: 1;
  }
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(26, 29, 46, 0.85) 0%,
    rgba(26, 29, 46, 0.3) 40%,
    transparent 100%
  );
}

.banner-info {
  position: absolute;
  bottom: $space-4;
  left: $space-4;
  right: $space-4;
  display: flex;
  align-items: center;
  gap: $space-3;
}

.banner-hex-avatar {
  @include hexagon(48px);
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid $mint-500;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.banner-text {
  flex: 1;
  min-width: 0;
}

.banner-name {
  font-family: $font-heading;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  @include ellipsis-1;
}

.banner-intro {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  @include ellipsis-1;
  margin-top: 2px;
}

.banner-dots {
  display: flex;
  justify-content: center;
  gap: $space-2;
  margin-top: $space-3;
}

.banner-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $ink-100;
  transition: all $dur-base $ease-out;
  cursor: pointer;

  &.active {
    width: 24px;
    border-radius: $radius-pill;
    background: $mint-500;
    box-shadow: 0 0 8px rgba(78, 205, 196, 0.5);
  }
}

// ---------- 圈子网格 ----------
.circle-grid-section {
  padding: $space-2 $space-4;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-4;
}

.section-title {
  font-family: $font-heading;
  font-size: 18px;
  font-weight: 700;
  color: $ink-900;
  display: flex;
  align-items: center;
  gap: $space-2;

  .hex-dot {
    width: 10px;
    height: 10px;
    background: $mint-500;
    clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  }
}

.section-count {
  font-size: 13px;
  color: $ink-300;
}

.circle-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $space-3;

  @media (min-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

.load-more-area {
  @include center(column);
  padding: $space-6 0;
  min-height: 60px;
}

.no-more {
  font-size: 13px;
  color: $ink-300;
}

// ---------- 创建圈子FAB ----------
.create-fab {
  position: fixed;
  right: $space-5;
  bottom: 100px;
  z-index: 90;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: $grad-mint;
  color: #fff;
  @include center;
  box-shadow: $shadow-md, $shadow-glow-mint;
  transition: all $dur-base $ease-spring;

  &:hover {
    transform: scale(1.1) rotate(90deg);
    box-shadow: $shadow-lg, 0 0 32px rgba(78, 205, 196, 0.6);
  }

  &:active {
    transform: scale(0.95);
  }
}

// ---------- 下拉刷新遮罩 ----------
.refresh-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
  @include center(column);
  gap: $space-2;
  padding: $space-4;
  background: rgba(245, 247, 251, 0.95);
  @include glass(12px, rgba(245, 247, 251, 0.9));
  font-size: 13px;
  color: $ink-500;
}

.refresh-hex {
  @include hexagon(32px);
  background: $grad-mint;
  @include center;
  color: #fff;
  animation: hex-spin 1s linear infinite;
}

// ---------- 过渡动画 ----------
.refresh-fade-enter-active,
.refresh-fade-leave-active {
  transition: all $dur-base $ease-out;
}

.refresh-fade-enter-from,
.refresh-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

// ---------- 响应式 ----------
@include mobile {
  .plaza-header {
    padding: $space-3 $space-3 $space-2;
  }

  .plaza-title {
    font-size: 18px;
  }

  .banner-carousel {
    aspect-ratio: 16 / 8;
  }

  .banner-name {
    font-size: 16px;
  }

  .circle-grid {
    gap: $space-2;
  }

  .create-fab {
    right: $space-4;
    bottom: 80px;
    width: 50px;
    height: 50px;
  }
}
</style>
