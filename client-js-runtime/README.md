# demo

本测试选择了最快的 js 运行时

## 1、安装 bun

[Bun](https://bun.sh/)

## 2、安装依赖

```sh
bun install
```

## 3、安装全局 PM2

```sh
bun i pm2 -g
```

## 4、运行 ready 脚本

```sh
./ready.sh
```

## 5、确定容器里的实例都已经启动

```sh
pm2 status
```

## 5、运行 start 脚本

```sh
./start.sh
```
