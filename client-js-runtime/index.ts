import { Client } from './client.ts'
import fs from 'fs'
import path from 'path'
import { Utils } from 'vue-fn/domain-server'

const { callback, promise } = Utils.createPromiseCallback(() => {})

const resultPath = path.join(__dirname, 'maxtime.log')
const signalPath = path.join(__dirname, 'signal.txt')
const client = new Client(process.env.HOST!)
const times = parseInt(process.env.TIMES_PER_INSTANCE!)

async function run() {
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

  const fileContent = parseInt(fs.readFileSync(resultPath, 'utf-8'))

  if (cost > fileContent) {
    fs.writeFileSync(resultPath, String(cost))
  }
  callback()
}

fs.watch(signalPath, async (eventType, _filename) => {
  if (eventType === 'change') {
    await run()
  }
})

await promise
