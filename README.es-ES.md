

# Referencia de archivos de librería
## Plataforma Windows
En la plataforma Windows, los archivos de librería terminan en `.dll`; en esta plataforma, simplemente copie los archivos de librería directamente en la raíz del proyecto.

Nota: La versión del SDK es `CH-HCNetSDKV5.3.6.30_build20180816_Win64`
## Plataforma Linux
En la plataforma Linux, los archivos de librería terminan en `.so`; la configuración para referenciar librerías en Linux es la siguiente:
1. Copie todos los archivos de librería de `libs/linux` a `/usr/hikvision/libs`, añada al final del archivo `/etc/ld.so.conf` la ruta de las librerías del SDK de red, por ejemplo `/XXX` y `/XXX/HCNetSDKCom/`, guarde los cambios y luego ejecute `ldconfig`.

        `/etc/ld.so.conf` 文件设置如下：
        ```editorconfig
        /usr/hikvision/libs 
        /usr/hikvision/libs/HCNetSDKCom
        
        include /etc/ld.so.conf.d/*.conf
        ```
2. `Agregue` `libctypto.so` y `libssl.so` a los directorios raíz correspondientes

   

Con estos pasos se completa la referencia de archivos de librería en la plataforma Linux.

Nota: La versión del SDK es `CH-HCNetSDKV6.0.2.2_build20181213_Linux64`

# Ejecución del programa
Para que el mismo código pueda ejecutarse en las plataformas Windows y Linux, es necesario realizar algunos ajustes en `HCNetSDK.java`:
1. Ajuste la relación de herencia en `HCNetSDK`
    ```java
 public interface HCNetSDK extends StdCallLibrary {}
 
 //修改内容： StdCallLibrary -> Library
 public interface HCNetSDK extends Library {}
 ```

2. Ajuste la relación de herencia en la declaración de las funciones de la API

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
3. En el servicio que utiliza `HCNetSDK`, determine la plataforma en ejecución para crear una instancia diferente de `HCNetSDK`:
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
