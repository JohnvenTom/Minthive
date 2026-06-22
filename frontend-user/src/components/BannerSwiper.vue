<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Swiper from 'swiper/bundle'
import { Autoplay, Pagination, EffectFade } from 'swiper/modules'
import 'swiper/swiper-bundle.css'
import type { Banner } from '@/api/config'
import { getBanners } from '@/api/config'

/**
 * 首页轮播图组件
 *
 * 功能描述：基于 Swiper.js 的首页轮播展示组件，支持自动播放、分页指示器、点击跳转，
 *           样式匹配薄荷蜂巢设计系统（毛玻璃、圆角、薄荷绿主题）
 * 参数：
 *   - 无 props，内部自行请求轮播数据
 * 返回值：
 *   - 无显式返回值，渲染轮播 DOM
 * 异常描述：
 *   - 接口请求失败时静默处理，不显示轮播区域
 * 注意事项：
 *   - 仅展示 status=ACTIVE 的轮播数据（后端已过滤）
 *   - linkUrl 为空时点击不跳转
 */

const router = useRouter()

/** 轮播数据列表 */
const banners = ref<Banner[]>([])
/** 是否有可用轮播 */
const hasBanners = ref(false)
/** Swiper 实例 */
let swiperInstance: Swiper | null = null
/** 容器引用 */
const swiperRef = ref<HTMLElement | null>(null)

/**
 * 加载轮播数据
 * @returns {Promise<void>}
 */
async function loadBanners(): Promise<void> {
  try {
    const res = await getBanners()
    const list: Banner[] = res.data || []
    banners.value = list
    hasBanners.value = list.length > 0
    // 数据更新后重新初始化 Swiper
    await initSwiper()
  } catch {
    // 静默失败，不显示轮播
    hasBanners.value = false
  }
}

/**
 * 初始化 / 更新 Swiper 实例
 * @returns {Promise<void>}
 */
async function initSwiper(): Promise<void> {
  // 销毁旧实例
  if (swiperInstance) {
    swiperInstance.destroy(true)
    swiperInstance = null
  }

  if (!hasBanners.value || !swiperRef.value || banners.value.length === 0) return

  // 等 DOM 更新后初始化
  await new Promise(resolve => setTimeout(resolve, 50))

  swiperInstance = new Swiper(swiperRef.value, {
    modules: [Autoplay, Pagination, EffectFade],
    loop: banners.value.length > 1,
    effect: 'slide',
    speed: 500,
    autoplay: banners.value.length > 1 ? {
      delay: 4000,
      disableOnInteraction: false,
      pauseOnMouseEnter: true,
    } : false,
    pagination: {
      el: '.banner-swiper-pagination',
      clickable: true,
      bulletActiveClass: 'banner-swiper-bullet-active',
    },
    slidesPerView: 1,
    spaceBetween: 0,
    observer: true,
    observeParents: true,
  })
}

/**
 * 点击轮播项跳转
 * @param {Banner} banner - 当前轮播项数据
 */
function handleBannerClick(banner: Banner): void {
  if (!banner.linkUrl) return
  // 判断是否为站内路由（以 / 开头）
  if (banner.linkUrl.startsWith('/')) {
    router.push(banner.linkUrl)
  } else {
    window.open(banner.linkUrl, '_blank')
  }
}

onMounted(() => {
  loadBanners()
})

onUnmounted(() => {
  if (swiperInstance) {
    swiperInstance.destroy(true)
    swiperInstance = null
  }
})

// 监听数据变化重建 Swiper
watch(hasBanners, () => {
  initSwiper()
})
</script>

<template>
  <Transition name="banner-fade">
    <div v-if="hasBanners" class="banner-swiper-wrapper">
      <div ref="swiperRef" class="banner-swiper">
        <div class="swiper-wrapper">
          <div
            v-for="item in banners"
            :key="item.id"
            class="swiper-slide banner-slide"
            @click="handleBannerClick(item)"
          >
            <img :src="item.imageUrl" :alt="item.title" class="banner-img" loading="lazy" />
            <!-- 渐变遮罩 + 标题 -->
            <div v-if="item.title" class="banner-overlay">
              <span class="banner-title">{{ item.title }}</span>
            </div>
          </div>
        </div>
        <!-- 分页指示器 -->
        <div class="banner-swiper-pagination" />
      </div>
    </div>
  </Transition>
</template>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/mixins.scss' as *;

.banner-swiper-wrapper {
  margin: $space-3 $space-4 0;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;

  @include mobile {
    margin: $space-2 $space-3 0;
  }
}

.banner-swiper {
  border-radius: $radius-md;
  overflow: hidden;
  position: relative;
  box-shadow: $shadow-sm;
  // 外层容器加一层微弱光晕
  &::before {
    content: '';
    position: absolute;
    inset: -1px;
    border-radius: calc(#{$radius-md} + 1px);
    background: linear-gradient(135deg, rgba(78, 205, 196, 0.25), rgba(107, 203, 119, 0.15), rgba(255, 182, 39, 0.1));
    z-index: 0;
    pointer-events: none;
  }

  :deep(.swiper-wrapper) {
    z-index: 1;
  }
}

.banner-slide {
  position: relative;
  cursor: pointer;
  aspect-ratio: 2.8 / 1;
  overflow: hidden;

  @include mobile {
    aspect-ratio: 2.2 / 1;
  }
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform $dur-base $ease-out;

  .banner-slide:hover & {
    transform: scale(1.03);
  }
}

.banner-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: $space-4 $space-5 $space-3;
  background: linear-gradient(to top, rgba(26, 29, 46, 0.7) 0%, transparent 100%);
  z-index: 2;
}

.banner-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.3px;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.35);
  @include ellipsis-1;

  @include mobile {
    font-size: 13px;
  }
}

// ---------- 分页指示器 ----------
.banner-swiper-pagination {
  position: absolute;
  right: $space-4;
  bottom: $space-3;
  left: auto;
  z-index: 10;
  display: flex;
  gap: 6px;

  :deep(.swiper-pagination-bullet) {
    width: 8px;
    height: 8px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.45);
    opacity: 1;
    transition: all $dur-fast $ease-out;
    flex-shrink: 0;
  }

  :deep(.banner-swiper-bullet-active) {
    width: 20px;
    background: $mint-500;
    box-shadow: 0 0 8px rgba(78, 205, 196, 0.5);
  }
}

// ---------- 进场动画 ----------
.banner-fade-enter-active {
  animation: fade-up 0.5s $ease-spring both;
}
.banner-fade-leave-active {
  animation: fade-up 0.3s $ease-out reverse both;
}
</style>
