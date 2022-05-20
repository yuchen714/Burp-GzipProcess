package burp;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;


public class BurpExtender implements IBurpExtender,  IHttpListener {
    private IExtensionHelpers helpers;//工具类
    private IBurpExtenderCallbacks callbacks;
    private IContextMenuInvocation currentInvocation;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks){
        this.callbacks = callbacks;
        callbacks.setExtensionName("Gzip_Response_decode");
        callbacks.printOutput("load success");
        callbacks.registerHttpListener(this);

    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) throws IOException {
        if (!messageIsRequest) { //如果是返回包，进行解密
            helpers = callbacks.getHelpers();
            IRequestInfo analyzeResponse = helpers.analyzeRequest(messageInfo);
            List<String> headers = analyzeResponse.getHeaders();
            byte[] new_Response ;
            /*****************获取body**********************/
            IResponseInfo analyzedResponse = helpers.analyzeResponse(messageInfo.getResponse()); //getResponse获得的是字节序列
            int bodyOffset = analyzedResponse.getBodyOffset();//响应包是没有参数的概念的，大多需要修改的内容都在body中
            byte[] response = messageInfo.getResponse();
            byte[] body = new byte[response.length-bodyOffset];
            System.arraycopy(response, bodyOffset, body, 0, response.length-bodyOffset);

            /*****************unzip**********************/
            byte[] unzip_byte_body = decompress(body);
            String requestS = new String(unzip_byte_body);
            callbacks.printOutput("unzip=============");
//            callbacks.printOutput(requestS);


            new_Response = helpers.buildHttpMessage(headers, unzip_byte_body);
            String test = new String(new_Response);
            callbacks.printOutput(test);
            messageInfo.setResponse(new_Response);


            IHttpRequestResponse[] selectedItems = this.currentInvocation.getSelectedMessages();
            byte[] selectedRequestOrResponse = selectedItems[0].getResponse();

            selectedItems[0].setResponse(new_Response); //替换目前使用的面板中的内容为处理后的字符串


        }
    }


    /**
     *对数据Gzip解密
     */
    protected byte[] decompress(byte[] compressed) throws IOException {
//        int bodyOffset = helpers.analyzeRequest(content).getBodyOffset();

        GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(compressed));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytes_read;

        while ((bytes_read = gzis.read(buffer)) > 0) {
            baos.write(buffer, 0, bytes_read);
        }
        baos.close();
        return baos.toByteArray();
    }

    /**
     *对数据Gzip加密
     */
    protected byte[] compress(byte[] content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos);
        gzos.write(content);
        gzos.flush();
        gzos.close();
        baos.close();
        return baos.toByteArray();
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

}
