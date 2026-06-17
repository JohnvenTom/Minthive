/**
 * 图片压缩工具
 * @module utils/compress
 * @description 前端压缩上传图片，单张 ≤5MB，使用 Canvas 实现
 */

/**
 * 压缩图片文件
 * @param {File} file - 原始图片文件
 * @param {object} options - 压缩参数
 * @param {number} options.maxSize - 最大尺寸（px），默认 1920
 * @param {number} options.quality - 压缩质量 0-1，默认 0.8
 * @returns {Promise<File>} 压缩后的 File 对象
 * @throws {Error} 文件类型不支持或压缩失败时抛出
 * @note 仅支持 jpeg/png/webp，GIF 直接返回原文件
 */
export function compressImage(
  file: File,
  options: { maxSize?: number; quality?: number } = {}
): Promise<File> {
  const { maxSize = 1920, quality = 0.8 } = options

  return new Promise((resolve, reject) => {
    if (!file.type.startsWith('image/')) {
      reject(new Error('仅支持图片文件'))
      return
    }
    // GIF 不压缩
    if (file.type === 'image/gif') {
      resolve(file)
      return
    }

    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img
        // 等比缩放到 maxSize 内
        if (width > maxSize || height > maxSize) {
          if (width > height) {
            height = (height * maxSize) / width
            width = maxSize
          } else {
            width = (width * maxSize) / height
            height = maxSize
          }
        }
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Canvas 上下文不可用'))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('压缩失败'))
              return
            }
            const compressed = new File([blob], file.name, {
              type: file.type,
              lastModified: Date.now()
            })
            resolve(compressed)
          },
          file.type,
          quality
        )
      }
      img.onerror = () => reject(new Error('图片加载失败'))
      img.src = e.target?.result as string
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

/**
 * 校验视频文件（时长 ≤60s，大小 ≤20MB）
 * @param {File} file
 * @returns {Promise<boolean>} 是否通过校验
 */
export function validateVideo(file: File): Promise<boolean> {
  return new Promise((resolve) => {
    if (file.size > 20 * 1024 * 1024) {
      resolve(false)
      return
    }
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      resolve(video.duration <= 60)
    }
    video.onerror = () => resolve(false)
    video.src = URL.createObjectURL(file)
  })
}
