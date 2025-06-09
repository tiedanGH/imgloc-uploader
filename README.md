# imgloc 图床上传插件

> 一个基于 [imgloc](https://imgloc.com/) API，使用 [Mirai Console](https://github.com/mamoe/mirai) 实现的图片上传插件。

## 主要功能
- 将自定义图片上传至图床，返回图片链接
- 已上传的所有图片可在 imgloc 个人账号下进行预览和管理
- 从任意图片链接下载并查看图片

## 安装插件
1. 将本项目 [releases](https://github.com/tiedanGH/imgloc-uploader/releases) 中的`.jar`文件放入`.\plugins\`目录下即可加载插件。
2. 在 imgloc 注册账号，并将 [https://imgloc.com/settings/api](https://imgloc.com/settings/api) 中的 API key 填入 `.\config\com.tiedan.ImglocUploader\Config.yml` 中
3. 重新启动 `MiraiConsole` 就可以开始使用啦~

## 指令帮助

| Command                     | Description |
|:----------------------------|:------------|
| `/upload <引用图片>`            | 上传引用消息中的图片  |
| `/upload <图片> [图片] [图片]...` | 批量上传多张图片    |
| `/upload <链接> [链接] [链接]...` | 从链接批量上传图片   |
| `/upload history`           | 查看近期上传历史    |
| `/download <链接>`            | 从图片链接下载图片   |

#### 接口限制：imgloc图床1小时内最多可以上传120张图片