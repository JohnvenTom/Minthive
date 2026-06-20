<template>
  <span class="anim-num" :class="{ 'anim-num--bump': bumping }">
    <Transition name="anim-num-roll">
      <span class="anim-num__inner" :key="formatted">
        {{ formatted }}
      </span>
    </Transition>
  </span>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { formatNumber } from '@/utils/format'

const props = defineProps<{ value: number }>()

const formatted = computed(() => formatNumber(props.value))
const bumping = ref(false)

watch(() => props.value, (newVal, oldVal) => {
  if (newVal === oldVal) return
  bumping.value = true
  setTimeout(() => { bumping.value = false }, 500)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.anim-num {
  display: inline-block;
  position: relative;
  overflow: hidden;
  height: 1em;
  line-height: 1;
}

.anim-num--bump {
  animation: anim-num-pop 0.45s $ease-spring;
}

@keyframes anim-num-pop {
  0%   { transform: scale(1); }
  35%  { transform: scale(1.22); }
  100% { transform: scale(1); }
}

.anim-num__inner {
  display: block;
  will-change: transform, opacity;
}

.anim-num-roll-enter-active {
  transition:
    transform 0.44s $ease-spring,
    opacity 0.26s ease-out;
}

.anim-num-roll-leave-active {
  transition:
    transform 0.26s cubic-bezier(0.55, 0, 1, 0.45),
    opacity 0.18s ease-in;
  position: absolute;
  inset: 0;
}

.anim-num-roll-enter-from {
  transform: translateY(105%) scale(1.15);
  opacity: 0;
}

.anim-num-roll-leave-to {
  transform: translateY(-105%) scale(0.85);
  opacity: 0;
}
</style>
