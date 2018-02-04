# 编译版本使用具体步骤 #
下载bin目录Hacker-xx.zip,运行run.bat，根据控制台提示，全自动或人工辅助方式玩“跳一跳”。若使用过程遇到问题，请详读如下内容。


## 手机连接 ##
1. 准备电脑一台，USB数据线一条，注意，必须是数据线，充电线是连接不上的。简单区分可通过数据线上标志来区分，若为闪电标志，则证明此为充电线，不具备数据传输功能。

2. 手机通过数据线连接电脑，手机驱动请自行根据相应机型百度安装。

3. 手机设置中开启开发者模式，并同时打开USB调试、模拟点击等开关。此步骤不同手机操作方式不同，部分手机在手机设置可直接找到相应选项，有些手机，比如小米，默认则不显示该选项，需要到MIUI版本号处连续点几下，出现提示后再到设置中才可找到开发者模式开关。此步骤请自行百度完成开启。

4. 若电脑已安装adb工具，则可以直接运行输入cmd打开命令窗口。若电脑未安装，可下载该程序中bin目录下Hacker-xx.zip，解压得到adb，然后按住Shift键+鼠标右键，会出现“在此处打开命令窗口”选项（或者自己切换到该目录亦可），打开即可。输入“adb devides”，查看手机是否连接成功（依赖于步骤3），若出现如下界面，则证明连接成功。否则执行步骤5操作。
![adb连接成功界面](https://i.imgur.com/Bi1HE3M.png)

5. 若第4步未显示设备，可打开电脑设备管理器查看（电脑右键属性即有该选项），是否未安装ADB interfacae驱动，此时可下载该程序中bin目录下Hacker-xx.zip，解压得到adb_interface_usb_driver.zip并解压至任意目录，待完装完驱动后删除。设备管理器选择相应设备 -> “选择浏览计算机以查找驱动程序软件” -> “从计算机的设备驱动列表中选择” -> “从磁盘安装” -> 浏览选择择解压得到的“android_winusb.inf”文件确定即可开始安装驱动。安装完成后，再次尝试第4步。若仍未成功，请确认第3步。

6. 解压“adb1.0.32.zip”，此压缩包中包含adb.exe及AdbWinUsbApi.dll、AdbWinApi.dll。若程序运行过程出现上述某个dll未安装时，将其拷贝到运行目录下即可。至此，大功告成！


## 运行程序 ##
1. 手机连接完成后，双击run.bat，即可开始游戏。如下图
![程序运行界面](https://i.imgur.com/TzQ5xAI.png)

2. 若手机为1080*1920分辨率，则不必进行修改即可直接运行。

3. 若手机为其它分辨率，则需要修改Phone.java参数以适应。保证如下原则即可：
	- 调整width/height参数，或者getBeginPoint/getEndPoint方法使得坐标区域为如下部分
	![最佳坐标区域](https://i.imgur.com/QxkooH4.png)
	
	- 或者运行目录添加config.properties文件，一行一键值对。内容如下：
	![自定义配置](https://i.imgur.com/UwoUQgs.png)
	
	- 如上配置，可直接通过工具得到像素点并填入，比如PhotoShop

4. 运行过程，请根据情况手机情况修改弹跳系数，运行模式等。为保险起见，结束分数时，可拔掉数据线，或者修改为手动模式，然后自己手动跳几下，以防分数无法上传，如下图，是笔者测试时，突然来了个电话，导致游戏结束，同时也刚好碰见分数无法上传情况。
![分数无法提交界面](https://i.imgur.com/KKXF4D5.png)

6. 运行过程，若为手动模式，请注意按下Enter键的时机，出现干扰时，稍后再发送执行命令，比如出现音乐符，有可能会影响程序视别。

5. Enjoy yourself！