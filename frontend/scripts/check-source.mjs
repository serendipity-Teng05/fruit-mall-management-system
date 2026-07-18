import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'

const sourceRoot = path.resolve('src')
const issues = []

const visit = async directory => {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const fullPath = path.join(directory, entry.name)
    if (entry.isDirectory()) await visit(fullPath)
    if (!entry.isFile() || !/\.(js|vue)$/.test(entry.name)) continue
    const content = await readFile(fullPath, 'utf8')
    const relative = path.relative(process.cwd(), fullPath)
    if (/https?:\/\/(localhost|127\.0\.0\.1)/.test(content)) issues.push(`${relative}: 源码中不能硬编码本地服务地址`)
    if (/\.(only|skip)\s*\(/.test(content)) issues.push(`${relative}: 发现被跳过或独占执行的测试标记`)
    if (/password\s*[:=]\s*['"]123456['"]/.test(content)) issues.push(`${relative}: 发现硬编码演示密码`)
  }
}

await visit(sourceRoot)
if (issues.length) {
  console.error(issues.join('\n'))
  process.exit(1)
}
console.log('Frontend source checks passed.')

