# WechatJumpHacker

**微信跳一跳辅助、图像识别、自动学习（目前手动）修正弹跳系数、Java版**

距微信《跳一跳》虽上线已一月有余，但依然有不少人沉迷于此不可自拔，公交车上、地铁上、电梯里，都能发出打开这款游戏魔性的声音。都怪自己手残，每次最多也只能玩到百来分，不服啊，于是，休息之余也是应了朋友要求写下这款辅助，从此轻松霸榜！

具体识别算法，这里就不做总结了，有兴趣可以公众号回复索要源码查看。源码中部分参数是在笔者的手机上进行调试的（分辨率为1080），大家可以根据自己手机，进行相应修改。目前为止，中心点的识别无干扰情况下基本可以达到100%的准确性，只是测试感觉弹跳的系数玩的过程中会在变化，不知是不是微信的故意为之。不过此款辅助运行过程可以随时修改弹跳系数，问题倒也不是太大。下面简要介绍下原理：



# _**主要步骤**_

  
**第一步：识别玩家位置**

由于瓶子颜色比较特殊，除了部分区域有高光外，其它都还算比较接近，在误差允许范围内波动不大。所以对于瓶子当前位置的识别做的比较简单，获取所有坐标点，然后取最下面的中间点即可。如下图中白色方格所示：

![](http://mmbiz.qpic.cn/mmbiz_png/3e0n8JBRmVJoz5BRdkLXVbr5xL2es5CBLnB7n5KmlwGibZDiaqesiaPst5Qlhn9IpRibVUFwW1Mb7ibuGVOnt0sfLLQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

**第二步：识别目标方块中心位置**

该步骤算是整个过程中最难点，因为会出现不同颜色，形状的图形，像音乐盒，魔方，甚至药瓶，总之越到后面，难度越大，精度也要求越高。大概思路为：移除背景色，再通过系列算法，比如瓶子头部为一个圆形，可以通过判断点是否在圆内来进行判断，然后得到目标块坐标集合，从而计算出中心点，如下图中红色方格所示：

![](http://mmbiz.qpic.cn/mmbiz_png/3e0n8JBRmVJoz5BRdkLXVbr5xL2es5CBLnB7n5KmlwGibZDiaqesiaPst5Qlhn9IpRibVUFwW1Mb7ibuGVOnt0sfLLQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

**第三步：识别目标方块中心白色圆点位置**

如果前一次踩中中心点，会有下一个中心点的提示，此步骤思路为在第二步骤的基础上，可能范围内寻找白色圆点，如果找到，则将第二步中找到的点替换掉，以精确寻找结果。如下图黑色圆点所示：

![](http://mmbiz.qpic.cn/mmbiz_png/3e0n8JBRmVJoz5BRdkLXVbr5xL2es5CBoyGUiaFeLJePlmaQiaaUjBgIIDzfGnDxThzsk4wKZsCbb3icBPr6WW8Jg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

以上三种坐标点的寻找，如果有哪种情形坐标点定位错误，程序均附有main方法可以进行调试，以准确定位问题所在。如下图：不同色块代表不同点集合，最终通过算法一一排除。红色中心点定位到上面黑色中的原因是由于白色中心点的存在，取Y轴坐标时取了中间点，但这个不是问题，最终坐标会根据白色点来确定。

![](http://mmbiz.qpic.cn/mmbiz_png/3e0n8JBRmVJoz5BRdkLXVbr5xL2es5CBibVRcsTUtVgpYvJl9SDOOlBuha0RfekhnJ4on1BcHxrJ06aMkTDYrew/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1)

# _**具体操作步骤**_

1 通过ADB截屏；

2 通过ADB将截屏保存到电脑；

3 识别瓶子位置；

4 识别目标方块中心位置；

5 识别目标方块中心白色圆点的位置；

如果第5步成功，则取第5步的中心点为下一步的位置；否则，取第4步的中心点为下一步的位置；

6 计算玩家位置与下一步的位置，乘以一定的系数，得到长按的时间；

7 通过ADB，触发长按；



# _**功能亮点**_

1、支持手动及自动两种游戏模式，自动模式下，随机休眠，模拟真实情况，以防分数无法提交。手动模式，时刻关注弹系数变化并修改，以提升成绩。

2、无论自动与手动均可以随时切换模式及随时修改弹跳系数。

注：该程序为小V休闲学习所写，请务用于其它不正当用途。如有需要打包程序体验或者源代码学习扩展，可以关注公众号，随时发消息联系索要。

![](http://mmbiz.qpic.cn/mmbiz_jpg/3e0n8JBRmVIKH9eM4flSJJPcmibqO6hsHeXy5UA1oI7LUOPV9ZABtRuN43GZEf1mrgymPleYMd3bMnVV2y95vQQ/640?wx_fmt=jpeg&tp=webp&wxfrom=5&wx_lazy=1)

![](http://mmbiz.qpic.cn/mmbiz_gif/3e0n8JBRmVJjha4usicjJwHPHDSbRpDbvccXZaV9icgWkhVSjPmmbJAbcx4DyPNRiaY8cJibIdkAcEYXRGUZP5bYHw/640?wx_fmt=gif&tp=webp&wxfrom=5&wx_lazy=1)

扫扫加关注，小Ⅴ帮带路！

