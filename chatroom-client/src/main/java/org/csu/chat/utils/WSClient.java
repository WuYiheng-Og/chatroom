package org.csu.chat.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.csu.chat.controller.AudioController;
import org.csu.chat.controller.ChatViewController;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WSClient extends WebSocketClient {

    public static WSStorage wsStorage;

    private static String loginUser="";
    private static WSClient instance;
    private static AudioController audioController; // 音频控制器
    private static ChatViewController chatViewController;
    public static synchronized WSClient getInstance() {
        if (instance == null) {
            Map<String, String> httpHeaders = new HashMap<>();
            wsStorage =WSStorage.getInstance();
            try {
                instance = new WSClient(new URI("ws://localhost:8888/websocket/"+loginUser));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return instance;
    }
    public WSClient(URI serverUri) {
        super(serverUri);
    }
    public static void setLoginUser(String loginUser) {
        WSClient.loginUser = loginUser;
    }
    public static void setController(Object o){
        if (o instanceof ChatViewController)
            WSClient.chatViewController = (ChatViewController) o;
        else if (o instanceof AudioController)
            WSClient.audioController = (AudioController) o;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("[websocket] 连接成功");
        System.err.println(instance.isOpen());
    }

    @Override
    public void onMessage(String message) {
        log.info("[websocket] 收到消息={}", message);
        JSONObject jsonObject = JSON.parseObject(message);

        // 记录发送语音的用户，和数据分开传输，免去处理字节流和字符串的转换了
        String audioFromUser = jsonObject.getString("audioFromUser");
        if (audioFromUser!=null){
            wsStorage.setAudioSender(audioFromUser);
            wsStorage.setAudioReceiver(jsonObject.getString("audioToUser"));
            wsStorage.getAudioHistoryList().add(audioFromUser);// 历史记录加一条
            return;
        }
        // 正常文本消息的发送
        wsStorage.setFromUsername(jsonObject.getString("username"));
        wsStorage.setOnlineNumber(jsonObject.getInteger("number"));
        wsStorage.setMessageType(jsonObject.getInteger("messageType"));
        wsStorage.setMessage(jsonObject.getString("textMessage"));
        JSONArray users = jsonObject.getJSONArray("onlineUsers");
        wsStorage.setOnlineUsers(users.toJavaList(String.class));//json对象转List<String>

        // 传给控制器处理消息
        int index = jsonObject.getInteger("controller");
        if (index != 0){
            handleMessage(index);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        wsStorage.setAudioByteArr(bytes.array());
        wsStorage.getAudioMap().put(wsStorage.getAudioHistoryList().size()-1,bytes.array());// 记录语音条，便于信息提取
//        handleMessage(1); TODO 暂时不用，但是如果是实时语音（其实应该说是对讲机可以打开使用)
        chatViewController.handleAudioMessage();
    }


    private void handleMessage(Integer index){
        switch (index){
            case 1:
                audioController.handleWSMessage();
                break;
            case 2:
                if (chatViewController == null)// 登录阶段只是上线但是没有聊天，故设为空
                    break;
//                chatroomViewController.handleWSMessage();
                chatViewController.handleWSMessage();
                break;
            default:
                break;
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        log.info("[websocket] 退出连接");
        instance = null;
        // 为了处理JavaFx中在非Fx线程要执行Fx线程相关的任务，必须在 Platform.runlater 中执行
//        ProcessChain.create().addRunnableInPlatformThread(() -> {
//            ApplicatonStore.clearPermissionInfo();
//            FlowHandler flowHandler= (FlowHandler) ApplicationContext.getInstance().getRegisteredObject("ContentFlowHandler");
//            try {
//                flowHandler.navigateTo(LoginController.class);
//            } catch (VetoException e) {
//                e.printStackTrace();
//            } catch (FlowException e) {
//                e.printStackTrace();
//            }
//        }).run();
    }

    @Override
    public void onError(Exception e) {
        log.info("[websocket] 连接错误={}", e.getMessage());
        instance = null;
    }
}
