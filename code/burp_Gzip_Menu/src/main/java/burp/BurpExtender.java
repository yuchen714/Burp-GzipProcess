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


public class BurpExtender implements IBurpExtender, IContextMenuFactory, ActionListener {
    private IExtensionHelpers helpers;//工具类
    private IBurpExtenderCallbacks callbacks;
    private IContextMenuInvocation currentInvocation;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks){
        this.callbacks = callbacks;
        callbacks.setExtensionName("Gzip");
        callbacks.printOutput("load success");
        callbacks.registerContextMenuFactory(this);

    }

    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {

        //判断是否是可以进行数据修改的burp模块
        if (invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST ||
                invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_RESPONSE) {

            this.currentInvocation = invocation;

            List<JMenuItem> listMenuItems = new ArrayList<JMenuItem>();
            //如果是可以进行数据修改的burp模块，添加unGzip、enGzip两个按钮
            JMenuItem jMenuItem1 = new JMenuItem("unGzip");
            jMenuItem1.setActionCommand("unGzip");
            jMenuItem1.addActionListener(this); //BurpExtender类（this）实现了actionPerformed()方法，本质上是绑定了actionPerformed()方法作为按钮触发时执行的方法


            JMenuItem jMenuItem2 = new JMenuItem("enGzip");
            jMenuItem2.setActionCommand("enGzip");
            jMenuItem2.addActionListener(this);

            listMenuItems.add(jMenuItem1);
            listMenuItems.add(jMenuItem2);

            return listMenuItems;
        } else if (invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST ||
                invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE) {

            this.currentInvocation = invocation;

            List<JMenuItem> listMenuItems = new ArrayList<JMenuItem>();
            //如果是不能进行数据修改的burp模块，添加unGzip2、enGzip2两个按钮

            JMenuItem jMenuItem3 = new JMenuItem("unGzip2");
            jMenuItem3.setActionCommand("unGzip_2");
            jMenuItem3.addActionListener(this);

            JMenuItem jMenuItem4 = new JMenuItem("enGzip2");
            jMenuItem4.setActionCommand("enGzip_2");
            jMenuItem4.addActionListener(this);

            listMenuItems.add(jMenuItem3);
            listMenuItems.add(jMenuItem4);
            return listMenuItems;
        } else {
            return null;
        }

    }


    /**
     *向按钮绑定的方法
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        String buildArgResult = "";
        String methodtag = "";

        if (command.equals("unGzip")) { //如果是可以进行编辑的模块，替换选中的内容
            IHttpRequestResponse[] selectedItems = this.currentInvocation.getSelectedMessages();
            int[] selectedBounds = this.currentInvocation.getSelectionBounds();
            byte selectedInvocationContext = this.currentInvocation.getInvocationContext();

            try {
                byte[] selectedRequestOrResponse = null;
                if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
                    selectedRequestOrResponse = selectedItems[0].getRequest();
                } else {
                    selectedRequestOrResponse = selectedItems[0].getResponse();
                }

                byte[] preSelectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, 0, selectedBounds[0]);
                byte[] selectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, selectedBounds[0], selectedBounds[1]);
                byte[] postSelectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, selectedBounds[1], selectedRequestOrResponse.length);

                byte[] newRequest = this.decompress(selectedPortion); //进行数据处理
                newRequest = addBytes(preSelectedPortion, newRequest);
                newRequest = addBytes(newRequest, postSelectedPortion);//将处理后的数据和其他没选中的数据进行拼接
                selectedItems[0].setRequest(newRequest); //替换目前使用的面板中的内容为处理后的字符串
                callbacks.printOutput(new String(newRequest));

            } catch (Exception err) {

                callbacks.printOutput("Exception with custom context application");

            }

        }else if (command.equals("unGzip_2")) { //如果是不能进行编辑的模块，弹出一个TextArea，显示解压后的内容


            IHttpRequestResponse[] selectedItems = currentInvocation.getSelectedMessages();
            int[] selectedBounds = currentInvocation.getSelectionBounds();
            byte selectedInvocationContext = currentInvocation.getInvocationContext();

            try {

                byte[] selectedRequestOrResponse = null;
                if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST) {
                    selectedRequestOrResponse = selectedItems[0].getRequest();
                } else {
                    selectedRequestOrResponse = selectedItems[0].getResponse();
                }

                byte[] selectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, selectedBounds[0], selectedBounds[1]);
                String result = new String(this.decompress(selectedPortion)); //进行数据处理

                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {

                        JTextArea ta = new JTextArea(10, 30);
                        ta.setText(result);
                        ta.setWrapStyleWord(true);
                        ta.setLineWrap(true);
                        ta.setCaretPosition(0);
                        ta.setEditable(false);
                        //弹出一个TextArea，显示处理后的数据
                        JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation response", JOptionPane.INFORMATION_MESSAGE);

                    }

                });
            } catch (Exception err) {
                callbacks.printOutput("Exception with custom context application");
            }
        }else if (command.equals("enGzip")) {
            IHttpRequestResponse[] selectedItems = this.currentInvocation.getSelectedMessages();
            int[] selectedBounds = this.currentInvocation.getSelectionBounds();
            byte selectedInvocationContext = this.currentInvocation.getInvocationContext();

            try {
                byte[] selectedRequestOrResponse = null;
                if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
                    selectedRequestOrResponse = selectedItems[0].getRequest();
                } else {
                    selectedRequestOrResponse = selectedItems[0].getResponse();
                }

                byte[] preSelectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, 0, selectedBounds[0]);
                byte[] selectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, selectedBounds[0], selectedBounds[1]);
                byte[] postSelectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, selectedBounds[1], selectedRequestOrResponse.length);

                byte[] newRequest = this.compress(selectedPortion);
                newRequest = addBytes(preSelectedPortion, newRequest);
                newRequest = addBytes(newRequest, postSelectedPortion);
                selectedItems[0].setRequest(newRequest);
                callbacks.printOutput(new String(newRequest));

            } catch (Exception err) {

                callbacks.printOutput("Exception with custom context application");

            }

        }else if (command.equals("unGzip_2")) {


            IHttpRequestResponse[] selectedItems = currentInvocation.getSelectedMessages();
            int[] selectedBounds = currentInvocation.getSelectionBounds();
            byte selectedInvocationContext = currentInvocation.getInvocationContext();

            try {

                byte[] selectedRequestOrResponse = null;
                if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST) {
                    selectedRequestOrResponse = selectedItems[0].getRequest();
                } else {
                    selectedRequestOrResponse = selectedItems[0].getResponse();
                }

                byte[] selectedPortion = Arrays.copyOfRange(selectedRequestOrResponse, selectedBounds[0], selectedBounds[1]);
                String result = new String(this.compress(selectedPortion));

                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {

                        JTextArea ta = new JTextArea(10, 30);
                        ta.setText(result);
                        ta.setWrapStyleWord(true);
                        ta.setLineWrap(true);
                        ta.setCaretPosition(0);
                        ta.setEditable(false);

                        JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation response", JOptionPane.INFORMATION_MESSAGE);

                    }

                });
            } catch (Exception err) {
                callbacks.printOutput("Exception with custom context application");
            }
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
