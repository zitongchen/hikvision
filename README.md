# 库文件的引用
## Window 平台
window 平台中库文件以 `.dll` 结尾；在 window 平台中，直接把库文件复制到项目的根目录中。

注：SDK 版本为 `CH-HCNetSDKV5.3.6.30_build20180816_Win64`
## Linux 平台
Linux 平台中库文件以 `.so` 结尾；在 linux 平台中引用库文件的配置如下：
1. 把 libs/linux 下的库文件全部复制到 `/usr/hikvision/libs`, 在`/etc/ld.so.conf`文件结尾添加网络sdk库的路径，如 /XXX 和 /XXX/HCNetSDKCom/，保存之后，然后执行`ldconfig`。

        `/etc/ld.so.conf` 文件设置如下：
        ```editorconfig
        /usr/hikvision/libs 
        /usr/hikvision/libs/HCNetSDKCom
        
        include /etc/ld.so.conf.d/*.conf
        ```
2. 把 `libctypto.so` 和 `libssl.so` 添加到相当的根目录中

   

通过上述步骤边完成便 Linux 平台上库文件的引用。

注：SDK 版本为 `CH-HCNetSDKV6.0.2.2_build20181213_Linux64`

# 程序运行
为了可以让同一份代码可以在 Window 和 Linux 平台上面运行需要对 `HCNetSDK.java` 进行一些调整：
1. 调整 `HCNetSDK` 中的继承关系
    ```java
    public interface HCNetSDK extends StdCallLibrary {}
    
    //修改内容： StdCallLibrary -> Library
    public interface HCNetSDK extends Library {}
    ```

2. 调整 API 函数声明中的继承关系

    ```java
  
    public static interface FRealDataCallBack_V30 extends StdCallCallback {
            public void invoke(NativeLong lRealHandle, int dwDataType,
                    ByteByReference pBuffer, int dwBufSize, Pointer pUser);
   }

    //修改内容： StdCallCallback -> Callback
    public static interface FRealDataCallBack_V30 extends Callback {
                public void invoke(NativeLong lRealHandle, int dwDataType,
                        ByteByReference pBuffer, int dwBufSize, Pointer pUser);
   }
    ```
3. 在使用 `HCNetSDK` 的服务里面，通过判断运行的平台从而选择创建不同的 `HCNetSDK` 实例
    ```java
    public class HikvisionService {
        // 根据不同的平台创建不同的 HCNetSDK 实例 
        private static HCNetSDK hCNetSDK;
        {
            if(Platform.isWindows()){
                hCNetSDK = (HCNetSDK) Native.loadLibrary("HCNetSDK",
                        HCNetSDK.class);
            }
            if(Platform.isLinux()){
                hCNetSDK = (HCNetSDK) Native.loadLibrary("hcnetsdk",
                        HCNetSDK.class);
            }
        }
        // 业务逻辑 ....
   }
    ```