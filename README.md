# liveroom-topics-android

> 国内用户推荐去码云下载，速度更快 [https://gitee.com/zegodev/liveroom-topics-android.git](https://gitee.com/zegodev/liveroom-topics-android.git)

1.项目代码没有包含 ZEGO SDK，需要下载相应的 SDK，引入到项目，才能运行项目。

请下载 Android SDK，解压得到 jar 和各架构下的 so，然后放入 src/LiveRoomPlayground/main/libs/ 目录。

2.src/LiveRoomPlayground/common 模块中的 GetAppIdConfig.java 中需要填写正确的 appID 和 appSign，若无，请在即构管理控制台申请。

3.如果需要体验外部滤镜，需要在 authpack.h 文件中填写正确的 faceUnity 的证书，若无需要去相芯科技申请，相芯科技申请美颜鉴权证书的指引网址：http://www.faceunity.com/docs_develop/#/markdown/integrate/flow_an）。

4.本 Demo 包含了声浪（频率功率谱）模块，若需要体验，请先联系 ZEGO 技术支持获取带声浪（频率功率谱）功能的 SDK。

## 快速开始  
### [推流](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/publish)  
### [拉流](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/play)  
## 常用功能  
### [多人视频通话](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/videoCommunicaton) 
### [直播连麦](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/joinLive)
### [多路混流](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/mixstream) 
## 进阶功能  
### [媒体次要信息](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/mediasideinfo)
### [媒体播放器](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/mediaplayer)
### [视频外部渲染](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/videoexternalrender)
### [视频外部采集](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/videocapture)
### [自定义前处理-FaceUnity](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/videofilter)
### [变声/混响/立体声](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/soundProcessing)
### [音频频谱与声浪](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/frequencySpectrum)
### [分层视频编码](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/layeredcoding)
### [本地媒体录制](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/mediarecorder)
### [混音](https://github.com/zegodev/liveroom-topics-android/tree/master/src/LiveRoomPlayground/mixing)
