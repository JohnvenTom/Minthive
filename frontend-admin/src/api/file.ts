import { getToken } from './request'

export async function uploadFile(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await fetch('/api/file/upload', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${getToken()}`
    },
    body: formData
  })
  const json = await res.json()
  if (json.code === 200) {
    return json.data as string
  }
  throw new Error(json.msg || '上传失败')
}
