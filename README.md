# ErrorZero

一个本地运行的安卓错题本，Android 8.0 及以上可用。

## 功能

- 按科目管理错题。预置九科，可以新增、重命名、排序和删除。
- 每道题可以上传多张题目图片和多张答案图片，支持拍照或从相册选择。拍照后直接进入裁剪，已添加的图片也能再次裁剪。
- 记录时间默认是当前时间，日期和时间都可以修改。
- 错因标签和模块标签都可以多选，可以新建，也可以从历史标签里选。标签支持重命名和合并。
- 每道题可以写备注。
- 重做提醒按艾宾浩斯间隔 1 / 2 / 4 / 7 / 15 / 30 天安排。点"已掌握"进入下一轮，点"仍不会"回到第一轮。每道题的重做日期都能单独调整。每天在设定时间推送一条通知，告诉你当天有多少道题待重做。
- 题目可以归档，归档后不再提醒。
- 详情页默认隐藏答案，先独立重做，再点开核对。
- 数据可以导出为 zip，导入时可选"合并"或"覆盖"。换手机时在旧手机导出，在新手机用"覆盖"导入即可。

## 备份格式

```
ErrorZero-备份-20261002-1600.zip
├── data.json      科目、标签、错题、复习记录
└── images/        题目与答案图片
```

## 构建

需要 JDK 17 和 Android SDK 34。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest    # 运行单元测试
```

Release 包使用 `errorzero-release.jks` 签名，密码在 `keystore.properties` 里。这两个文件请妥善保管。以后发布新版本必须用同一把密钥签名，才能覆盖安装而不丢数据。

## 技术栈

Kotlin、Jetpack Compose（Material 3）、Room、WorkManager、Coil、kotlinx.serialization，裁剪使用 [Android-Image-Cropper](https://github.com/CanHub/Android-Image-Cropper)。
