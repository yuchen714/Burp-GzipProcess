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
        callbacks.setExtensionName("Gzip_Request_encode");
        callbacks.printOutput("load success");
        callbacks.registerHttpListener(this);

    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) throws IOException {
         if (messageIsRequest) { //如果是请求包，进行加密
            helpers = callbacks.getHelpers();
            IRequestInfo analyzeRequest = helpers.analyzeRequest(messageInfo);
            List<String> headers = analyzeRequest.getHeaders();
            byte[] new_Request ;
            /*****************获取body**********************/
            int bodyOffset = analyzeRequest.getBodyOffset();
            byte[] byte_Request = messageInfo.getRequest();

            String request = new String(byte_Request); //byte[] to String
            String body = request.substring(bodyOffset);
            byte[] byte_body = body.getBytes();


            byte[] enzip_byte_body = compress(byte_body);
            String enzip_request = new String(enzip_byte_body);
            callbacks.printOutput("enzip=============");
            callbacks.printOutput(enzip_request);
            new_Request = helpers.buildHttpMessage(headers, enzip_byte_body);
            messageInfo.setRequest(new_Request);
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
