import * as echarts from 'echarts'

const palette = ['#E879A9', '#F59E0B', '#34D399', '#A78BFA', '#EF4444', '#F97316', '#06B6D4', '#EC4899']

export function registerMintHiveTheme() {
  echarts.registerTheme('minthive', {
    color: palette,
    backgroundColor: 'transparent',
    textStyle: { color: '#B5BDD4' },
    title: { textStyle: { color: '#F4F4F5' }, subtextStyle: { color: '#A1A1AA' } },
    line: { itemStyle: { borderWidth: 2 }, lineStyle: { width: 2 }, symbolSize: 6, symbol: 'circle', smooth: true },
    bar: { itemStyle: { barBorderWidth: 0, barBorderRadius: [4, 4, 0, 0] } },
    pie: { itemStyle: { borderWidth: 2, borderColor: '#27272a' } },
    categoryAxis: {
      axisLine: { show: true, lineStyle: { color: '#3A4270' } },
      axisTick: { show: false },
      axisLabel: { color: '#7A86B8' },
      splitLine: { show: false }
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#7A86B8' },
      splitLine: { lineStyle: { color: '#2F3658', type: 'dashed' } }
    },
    legend: { textStyle: { color: '#B5BDD4' } },
    tooltip: {
      backgroundColor: 'rgba(39, 39, 42, 0.95)',
      borderColor: '#3f3f46',
      textStyle: { color: '#F4F4F5' },
      extraCssText: 'border-radius: 8px; box-shadow: 0 4px 24px rgba(0,0,0,0.4);'
    }
  })
}

export function registerMintHiveLightTheme() {
  echarts.registerTheme('minthive-light', {
    color: palette,
    backgroundColor: 'transparent',
    textStyle: { color: '#52525b' },
    title: { textStyle: { color: '#18181b' }, subtextStyle: { color: '#71717A' } },
    line: { itemStyle: { borderWidth: 2 }, lineStyle: { width: 2 }, symbolSize: 6, symbol: 'circle', smooth: true },
    bar: { itemStyle: { barBorderWidth: 0, barBorderRadius: [4, 4, 0, 0] } },
    pie: { itemStyle: { borderWidth: 2, borderColor: '#E4E4E7' } },
    categoryAxis: {
      axisLine: { show: true, lineStyle: { color: '#D4D4D7' } },
      axisTick: { show: false },
      axisLabel: { color: '#71717A' },
      splitLine: { show: false }
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#71717A' },
      splitLine: { lineStyle: { color: '#E4E4E7', type: 'dashed' } }
    },
    legend: { textStyle: { color: '#52525b' } },
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E4E4E7',
      textStyle: { color: '#18181b' },
      extraCssText: 'border-radius: 8px; box-shadow: 0 4px 24px rgba(0,0,0,0.1);'
    }
  })
}
