# Burp-GzipProcess
一个Gzip场景下帮助渗透测试的Burp插件，会对Gzip数据进行各种处理 (一般用于富客户端渗透测试)

使用方式：
Gzip_Menu.jar; Gzip_Request_encode.jar; Gzip_Response_decode.jar 是编译好的插件

Gzip_Menu.jar加载后可以选中需要压缩/解压的数据，右键选择unGzip/enGzip，对数据做压缩或者解压
 ![](https://github.com/yuchen714/Burp-GzipProcess/blob/main/1.png)
 
Gzip_Request_encode.jar 加载后对所有发出的数据包body做Gzip压缩
Gzip_Response_decode.jar 加载后对所有获取的数据包body做Gzip解压。
