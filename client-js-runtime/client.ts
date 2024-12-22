// import fetch, { type RequestInit } from 'node-fetch'

export class Client {
  private host: string

  constructor(host: string) {
    this.host = host
  }

  // 获取随机数
  async getRandom(top: number = 1000): Promise<number> {
    const url = `http://${this.host}/random?top=${top}`
    try {
      const response = await fetch(url)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const bodyText = await response.text()
      return parseInt(bodyText, 10) || 0
    } catch (error) {
      return Promise.reject(error)
    }
  }

  // 发送数字列表并获取结果
  async add(numbers: number[]): Promise<number> {
    const url = `http://${this.host}/add`
    const body = numbers.join(',')
    const options: RequestInit = {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: body,
    }

    try {
      const response = await fetch(url, options)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const bodyText = await response.text()
      return parseInt(bodyText, 10) || 0
    } catch (error) {
      return Promise.reject(error)
    }
  }
}
