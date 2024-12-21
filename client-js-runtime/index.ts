import { Client } from './client.ts'
import fs from 'fs'
import path from 'path'

const client = new Client(process.env.HOST!)
const times = parseInt(process.env.TIMES_PER_INSTANCE!)

const start = Date.now()
const tasks: Promise<void>[] = []
for (let i = 0; i < times; i++) {
  const a = client.getRandom()
  const b = client.getRandom()
  tasks.push(
    Promise.all([a, b]).then(([a, b]) => {
      client.add([a, b])
    })
  )
}

await Promise.all(tasks)
const cost = Date.now() - start
const logPath = path.join(__dirname, 'maxtime.log')
const fileContent = parseInt(fs.readFileSync(logPath, 'utf-8'))

if (cost > fileContent) {
  fs.writeFileSync(logPath, String(cost))
}
