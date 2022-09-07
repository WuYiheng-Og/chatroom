package org.csu.chat.controller;

import com.alibaba.fastjson.JSON;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.csu.chat.domain.DTO.WebSocketDTO;
import org.csu.chat.domain.VO.AudioMsg;
import org.csu.chat.domain.VO.Member;
import org.csu.chat.domain.VO.Message;
import org.csu.chat.utils.CONSTANT;
import org.csu.chat.utils.WSClient;
import org.csu.chat.utils.WSStorage;

import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

@FXMLController
@Slf4j
public class ChatViewController implements Initializable {

    @FXML
    private FlowPane membersList;
    @FXML
    private TextArea message;
    @FXML
    private FlowPane messagesList;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Button recordBtn;
    @FXML
    private ImageView recordImg;


    public static HashMap<String,Member> memberMaps = new HashMap<>();
//    public static ArrayList<Member> members = new ArrayList<Member>();
    public static HashMap<HBox,String> memberCardMaps = new HashMap<>();// 用户-卡片映射，便于寻找点击的卡片属于哪个用户
    public static HashMap<HBox,Integer> audioMsgMaps = new HashMap<>();// 音频-气泡消息映射，便于寻找点击的气泡属于哪个音频消息
    public static HashMap<String,Collection<Node>> userPanel = new HashMap<>();// 用户-面板消息列表映射，便于寻找对应的用户消息/群聊消息

    // 下面用于通讯
    private WSClient wsClient;// 客户端webSocket实体
    private WebSocketDTO webSocketDTO;// 通讯传输对象
    private WSStorage storage;  // 整个系统的缓存对象，便于信息的全局存取
    private AudioController audioController;// 用于控制语音播放
    private String curChatUser; // 当前对话的用户/群聊（为了在change前持久化）

    private boolean last = true;

    @Override
    public void initialize(URL url, ResourceBundle resources) {
//        membersList.setPadding(new Insets(5));
        VBox.setVgrow(messagesList, Priority.ALWAYS);


        // 文本框添加事件处理
        // 回车发送信息而不换行，但组合键 Ctrl + Enter 换行
        message.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getEventType() == KeyEvent.KEY_RELEASED && keyEvent.getCode() == KeyCode.ENTER){// 回车键清除 TODO 我真的不会搞这个，每次都有一个回车键
                    message.clear();
                    return;
                }
                // 如果按下了回车键
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    // 获得此时的光标位置。此位置为刚刚输入的换行符之后
                    int caretPosition = message.getCaretPosition();
                    // 如果已经按下的按键中包含 Control 键
                    if (!keyEvent.isControlDown()) { // 如果输入的不是组合键 `Ctrl+Enter`，去掉换行符，然后将文本发送
                        // 获得输入文本，此文本包含刚刚输入的换行符
                        String text = message.getText();
                        message.clear();// 清除文本框的文字
                        // 获得换行符两边的文本
//                        String front = text.substring(0, caretPosition - 1);
//                        String end = text.substring(caretPosition);
//                        message.setText(front + end);

                        // 信息发送
                        sendMessage(text);
                        Message theMessage = new Message(storage.getLoginUser(), text, new Timestamp(new Date().getTime()));
                        addMessageBox(theMessage);

                        /*----- 如果希望发送后保留输入框文本，需要只使用下面这行代码，然后去掉清除文本框的代码 -------*/
                        // message.positionCaret(caretPosition - 1);
                    } else {
                        // 获得输入文本，此文本不包含刚刚输入的换行符
                        String text = message.getText();
                        // 获得光标两边的文本
                        String front = text.substring(0, caretPosition);
                        String end = text.substring(caretPosition);
                        // 在光标处插入换行符
                        message.setText(front + System.lineSeparator() + end);
                        // 将光标移至换行符
                        message.positionCaret(caretPosition + 1);
                    }
                }
            }
        });

        // 消息可滑动
        scrollPane.vvalueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                if (last) {
                    scrollPane.setVvalue(1.0);
                    last = false;
                }
            }
        });

        // 与服务器建立连接
        getSocketInitialize();

        // 渲染左侧聊天列表
        addMemberGroup();
        // 渲染用户列表
        getPersonalData();
        addMembers();
        curChatUser = "群聊";

        scrollPane.setVvalue(1);
    }

    //初始化SocketClient服务
    private void getSocketInitialize (){
        // 客户端上线 TODO 测试用 记得删除
        String userTemp = RandomStringUtils.random(6, "abcdefgABCDEFG");
        WSClient.setLoginUser(userTemp);
        WSStorage.getInstance().setLoginUser(userTemp);
        WSClient.getInstance().connect();

        // 获得客户端socket
        wsClient = WSClient.getInstance();
        wsClient.setController(this);// 装填控制器对象，方便接收消息后对其控制
        audioController = new AudioController();
        wsClient.setController(audioController);
        // 获得WebSocket信息缓存对象
        storage = WSClient.wsStorage;
        // 初始化通信传输对象
        webSocketDTO = new WebSocketDTO();
        // 给服务器发消息获取在线列表信息
        webSocketDTO.setUsername(storage.getLoginUser());
        webSocketDTO.setToUserName(storage.getLoginUser());
        webSocketDTO.setMessage("");
        webSocketDTO.setType(CONSTANT.MessageToServerType.OFFLINE_USERLIST.getCode());
        webSocketDTO.setController(CONSTANT.ControllerChoose.CHAT_CONTROLLER.getCode());
        wsClient.send(JSON.toJSONString(webSocketDTO)); // 向服务端发送上线信息，服务端会回复在线列表信息
    }

    /**
     * 获取用户信息列表
     */
    private void getPersonalData() {
        int i=0;
        for (String user: storage.getOnlineUsers()){
//            members.add(new Member("http://orange-1312206514.cos.ap-guangzhou.myqcloud.com/images/test.jpg",user,true));
            memberMaps.put(user,new Member("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/avatar"+(i++)+".jpg",user,true));
        }

//        Message message1 = new Message(1, "那天你消失在人海里", new Timestamp(new Date().getTime()));
//        Message message2 = new Message(4, "你的背影沉默得让人恐惧 你说的那些问题 我回答得很坚定", new Timestamp(new Date().getTime()));
//        Message message3 = new Message(2, "偏偏那个时候我最想你", new Timestamp(new Date().getTime()));
//        Message message4 = new Message(2, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你", new Timestamp(new Date().getTime()));
//        Message message5 = new Message(5, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，", new Timestamp(new Date().getTime()));
//        Message message6 = new Message(2, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，", new Timestamp(new Date().getTime()));
//        Message message7 = new Message(4, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，那天你消失在人海里", new Timestamp(new Date().getTime()));
//        Message message8 = new Message(4, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，那天你消失在人海里那天你消失在人海里", new Timestamp(new Date().getTime()));
//        Message message9 = new Message(4, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，那天你消失在人海里那天你消失在人海里那天你消失在人海里那天你消失在人海里", new Timestamp(new Date().getTime()));
//        Message message10 = new Message(4, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，那天你消失在人海里那天你消失在人海里", new Timestamp(new Date().getTime()));
//        Message message11 = new Message(4, "我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，我不曾爱过你我不曾爱过你，我自己骗自己，已经给你写了信，又被我丢进海里，那天你消失在人海里那天你消失在人海里", new Timestamp(new Date().getTime()));
//
//        messages.add(message1);
//        messages.add(message2);
//        messages.add(message3);
//        messages.add(message4);
//        messages.add(message5);
//        messages.add(message6);
//        messages.add(message7);
//        messages.add(message8);
//        messages.add(message9);
    }

    /**
     * 渲染用户成员列表
     */
    private void addMembers() {
        for (Member member: memberMaps.values()) {
            if (member.getName().equals(storage.getLoginUser()))// 不要渲染自己在好友列表里，排除在外
                continue;
//            Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(member.getAvatar())));
            addMember(member);
        }
    }

    /**
     * 渲染群聊大厅
     */
    private void addMemberGroup() {
        // Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(member.getAvatar())));
        Image headImg = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/group.jpg");
        ImageView head = new ImageView();
        Platform.runLater(()->{
            head.setImage(headImg);
        });
        head.setFitWidth(40);
        head.setFitHeight(40);
//        Circle circle = new Circle(5);
//        circle.setFill(Color.rgb(255,0,0));
//        circle.setLayoutX(10);
//        circle.setLayoutY(10);
        head.setLayoutX(10);
        head.setLayoutY(10);
        AnchorPane headInfo = new AnchorPane();
        headInfo.getChildren().add(head);

        Label name = new Label("旺仔小馒头堆");
        name.setTextFill(Color.rgb(255, 255, 255));
        Label status = new Label("火热聊天中...");
        status.setTextFill(Color.rgb(255, 255, 255));
        VBox info = new VBox(8, name, status);// 把在线信息和昵称信息装填在info里
        info.setPadding(new Insets(10, 0, 10, 10));
        HBox groupInfo = new HBox(headInfo,info);
        groupInfo.setPrefWidth(messagesList.getPrefWidth());//边框大小和父容器一致

        // 添加选中样式（因为进来的第一个就是选中部分，所以选中）
        // 渲染当前卡片背景色
        groupInfo.setStyle("-fx-background-color: rgb(3,158,211)");

        // 定义一个Event Filter
        EventHandler<MouseEvent> filter = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                // 1.点自己不切换
                if (curChatUser.equals("群聊")){
                    return;
                }

                // b  去除群聊头像小红点
                // b.1 获取聊天对象卡片下的ap面板（存放小红点和头像）
                HBox groupCard = (HBox) event.getSource();// 获取群聊卡片
                AnchorPane ap = (AnchorPane) groupCard.getChildren().get(0);
                // b.2 去除卡片小红点
                try {
                    ap.getChildren().remove(1);// 若有小红点，去除
                }catch (Exception e){
                    log.info("没有小红点哦");
                } // 没有，就当无事发生

                // 2.持久化前一个页面
                Collection<Node> nodes = new ArrayList<>();
                for (Node node: messagesList.getChildren()){
                    nodes.add(node);
                }
                userPanel.put(curChatUser, (Collection<Node>) nodes); // 装填

                // 选中样式
                // 1.清空面板背景色
                for (Node n:membersList.getChildren()){
                    n.setStyle("-fx-background-color: rgb(39,43,45)");
                }
                // 2.渲染当前卡片背景色
                HBox curCard = (HBox) event.getSource();
                curCard.setStyle("-fx-background-color: rgb(3,158,211)");


                // 3.点击后切换到群聊chanel
                changePanel("群聊");
                curChatUser = "群聊";
            }
        };
        groupInfo.addEventFilter(MouseEvent.MOUSE_CLICKED,filter);

        Platform.runLater(()->{membersList.getChildren().add(groupInfo);});// 渲染

    }
    /**
     * 新增member并渲染
     */
    private void addMember(Member member){
        // Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(member.getAvatar())));
        Image headImg = new Image(member.getAvatar());
        ImageView head = new ImageView();
        Platform.runLater(()->{
            head.setImage(headImg);
        });
        head.setFitWidth(40);
        head.setFitHeight(40);
        // 采用锚布局方便加小红点
        head.setFitWidth(40);
        head.setFitHeight(40);
        head.setLayoutX(10);
        head.setLayoutY(10);
        AnchorPane headInfo = new AnchorPane();
        headInfo.getChildren().add(head);

        Label name = new Label(member.getName());
        name.setTextFill(Color.rgb(255, 255, 255));
        Label status = new Label(member.getStatus() ? "在线" : "离线");
        status.setTextFill(Color.rgb(255, 255, 255));
        VBox info = new VBox(8, name, status);// 把在线信息和昵称信息装填在info里
        info.setPadding(new Insets(10, 0, 10, 10));
        HBox userInfo = new HBox(headInfo,info);
        userInfo.setPrefWidth(messagesList.getPrefWidth());//边框大小和父容器一致
        memberCardMaps.put(userInfo,member.getName());// 存入，便于后续操作
        // 定义一个Event Filter
        EventHandler<MouseEvent> filter = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                // 获取当前点击卡片
                HBox curCard = (HBox) event.getSource();

                // 获取被选中对象的昵称
                String chosenUser = memberCardMaps.get(curCard);
                System.out.println(chosenUser);

                // 1.如果是自己，不切换
                if (chosenUser.equals(storage.getLoginUser()))// 点击同一个无效
                    return;
                // 1.1 如果是同样的聊天对象，不切换
                if (chosenUser.equals(curChatUser))// 点击同一个无效
                    return;

                // b 如果是不同的聊天对象，去除小红点
                // b.1 获取聊天对象卡片(上方已获得）下的ap面板（存放小红点和头像）
                AnchorPane ap = (AnchorPane) curCard.getChildren().get(0);
                // b.2 去除卡片小红点
                try {
                    ap.getChildren().remove(1);// 若有小红点，去除
                }catch (Exception e){
                    log.info("没有小红点哦");
                } // 没有，就当无事发生

                // 2.持久化前一个页面主界面
                Collection<Node> nodes = new ArrayList<>();
                for (Node node: messagesList.getChildren()){
                    nodes.add(node);
                }
                userPanel.put(curChatUser, (Collection<Node>) nodes); // 装填

                // 选中样式
                // 1.清空面板背景色
                for (Node n:membersList.getChildren()){
                    n.setStyle("-fx-background-color: rgb(39,43,45)");
                }
                // 2.渲染当前卡片背景色
                curCard.setStyle("-fx-background-color: rgb(3,158,211)");

                // 3.切换
                if (userPanel.get(chosenUser) == null){// 若不存在，则新建
                    userPanel.put(chosenUser,new ArrayList<>());
                }
                changePanel(chosenUser);
                curChatUser = chosenUser;
            }
        };

        userInfo.addEventFilter(MouseEvent.MOUSE_CLICKED,filter);


        Platform.runLater(()->{membersList.getChildren().add(userInfo);});
    }
    /**
     * 删除member
     */
    private void removeMember(String name){
        // 遍历寻找下线用户并删除
        for (HBox key:memberCardMaps.keySet()){
            if (memberCardMaps.get(key).equals(name)){
                Platform.runLater(()->{membersList.getChildren().remove(key);});
                return;
            }
        }
    }

    private void changePanel(String key){
        Platform.runLater(()->{
            messagesList.getChildren().clear();
            if (userPanel.get(key)==null) return;// 如果发现获得key为空，说明还没有消息，清屏退出即可
            messagesList.getChildren().addAll(userPanel.get(key));
        });
    }

    private Member getMemberByName(String senderName) {
        for (Member member: memberMaps.values()) {
            if (member.getName().equals(senderName)) {
                return member;
            }
        }
        return null;
    }

    /**
     * 渲染消息盒子
     */
    private void addMessageBox(Message message) {
        Member sender = getMemberByName(message.getSenderName());
        assert sender != null;
//        Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(sender.getAvatar())));
        Image headImg = new Image(sender.getAvatar());
        ImageView head = new ImageView();
        Platform.runLater(()->{
            head.setImage(headImg);
        });
        head.setFitWidth(40);
        head.setFitHeight(40);

        Label messageBubble = new Label(message.getMessage());
        messageBubble.setWrapText(true);
        messageBubble.setMaxWidth(220);
        messageBubble.setPadding(new Insets(6));
        messageBubble.setFont(new Font(14));
        HBox.setMargin(messageBubble, new Insets(8, 0, 0, 0));

        boolean isMine = message.getSenderName().equals(storage.getLoginUser());
        double[] points;
        if (!isMine) {
            points = new double[]{
                    0.0, 5.0,
                    10.0, 0.0,
                    10.0, 10.0
            };
        } else {
            points = new double[]{
                    0.0, 0.0,
                    0.0, 10.0,
                    10.0, 5.0
            };
        }
        Polygon triangle = new Polygon(points);

        HBox messageBox = new HBox();
        messageBox.setPrefWidth(366);
        messageBox.setPadding(new Insets(15, 5, 10, 5));
        if (isMine) {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(179,231,244); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(179,231,244));

            HBox.setMargin(triangle, new Insets(15, 10, 0, 0));
            // 添加user的布局位置
            Text username = new Text(storage.getLoginUser());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));
            Platform.runLater(()->{ messageBox.getChildren().addAll(messageBubble, triangle,userInfo );  });
//            messageBox.getChildren().addAll(messageBubble, triangle, head);
            messageBox.setAlignment(Pos.TOP_RIGHT);
        } else {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(255,255,255); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(255,255,255));

            HBox.setMargin(triangle, new Insets(15, 0, 0, 10));
            // 添加user的布局位置
            Text username = new Text(sender.getName());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));
            Platform.runLater(()->{ messageBox.getChildren().addAll(userInfo, triangle, messageBubble);  });
        }
        last = scrollPane.getVvalue() == 1.0;
        Platform.runLater(()->{
            messagesList.getChildren().add(messageBox);
        });
    }

    /**
     * 消息盒子加工厂（包装）
     */
    private Node getMessageBox(Message message){
        Member sender = getMemberByName(message.getSenderName());
        assert sender != null;
//        Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(sender.getAvatar())));
        Image headImg = new Image(sender.getAvatar());
        ImageView head = new ImageView();
        Platform.runLater(()->{
            head.setImage(headImg);
        });
        head.setFitWidth(40);
        head.setFitHeight(40);

        Label messageBubble = new Label(message.getMessage());
        messageBubble.setWrapText(true);
        messageBubble.setMaxWidth(220);
        messageBubble.setPadding(new Insets(6));
        messageBubble.setFont(new Font(14));
        HBox.setMargin(messageBubble, new Insets(8, 0, 0, 0));

        boolean isMine = message.getSenderName().equals(storage.getLoginUser());
        double[] points;
        if (!isMine) {
            points = new double[]{
                    0.0, 5.0,
                    10.0, 0.0,
                    10.0, 10.0
            };
        } else {
            points = new double[]{
                    0.0, 0.0,
                    0.0, 10.0,
                    10.0, 5.0
            };
        }
        Polygon triangle = new Polygon(points);

        HBox messageBox = new HBox();
        messageBox.setPrefWidth(366);
        messageBox.setPadding(new Insets(15, 5, 10, 5));
        if (isMine) {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(179,231,244); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(179,231,244));

            HBox.setMargin(triangle, new Insets(15, 10, 0, 0));
            // 添加user的布局位置
            Text username = new Text(storage.getLoginUser());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));
            Platform.runLater(()->{ messageBox.getChildren().addAll(messageBubble, triangle,userInfo );  });
//            messageBox.getChildren().addAll(messageBubble, triangle, head);
            messageBox.setAlignment(Pos.TOP_RIGHT);
        } else {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(255,255,255); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(255,255,255));

            HBox.setMargin(triangle, new Insets(15, 0, 0, 10));
            // 添加user的布局位置
            Text username = new Text(sender.getName());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));
            Platform.runLater(()->{ messageBox.getChildren().addAll(userInfo, triangle, messageBubble);  });
        }
        last = scrollPane.getVvalue() == 1.0;
        return messageBox;
    }
    /**
     *  提示消息，如用户上线下线等信息
     */
    private void addCommonMsg(String message){
        HBox messageBox = new HBox();
        Label messageBubble = new Label(message);
        // 居中显示
        messageBox.setPrefWidth(scrollPane.getPrefWidth());
        messageBubble.setPadding(new Insets(10,0,5,(scrollPane.getPrefWidth()-message.length() * 12)/2));

        messageBox.getChildren().add(messageBubble);
        // TODO 这种消息是群聊消息，要去群聊里渲染（我觉得也可以不做，因为相当于系统小喇叭，绝对不是我偷懒doge）

        Platform.runLater(()->{
            messagesList.getChildren().add(messageBox);
        });
    }

    /**
     * 发送消息到前端
     */
    private void sendMessage(String message) {
        if(message.equals("")){// 为空，不能发送消息
            return;
        }
        
        if(curChatUser.equals("群聊")){// 群发
            webSocketDTO.setMessage(message);
            webSocketDTO.setType(CONSTANT.MessageToServerType.PUBLIC_MESSAGE.getCode());
            webSocketDTO.setToUserName(storage.getLoginUser());
            webSocketDTO.setController(CONSTANT.ControllerChoose.CHAT_CONTROLLER.getCode());
            wsClient.send(JSON.toJSONString(webSocketDTO));
        }else {// 私信
            webSocketDTO.setMessage(message);
            webSocketDTO.setType(CONSTANT.MessageToServerType.PRIVATE_MESSAGE.getCode());
            webSocketDTO.setToUserName(curChatUser);
            webSocketDTO.setController(CONSTANT.ControllerChoose.CHAT_CONTROLLER.getCode());
            wsClient.send(JSON.toJSONString(webSocketDTO));
        }
        
    }

    /**
     * 处理ws消息
     */
    public void handleWSMessage(){
        System.out.println("我是ChatRoom，我在处理了哦");
        Integer messageType = storage.getMessageType();
        // 上线
        if(messageType == CONSTANT.MessageType.ONLINE_MESSAGE.getCode()){
            log.info(storage.getFromUsername()+"加入聊天室啦~");

            if (storage.getFromUsername().equals(storage.getLoginUser())){// 如果是自己，不用刷新在线列表
                addCommonMsg("欢迎"+storage.getLoginUser()+"进入聊天室~");
                return;
            }

            addCommonMsg(storage.getFromUsername()+"加入聊天室啦~");
            // 更新在线列表并显示
            Member member = new Member("http://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/avatar"+(storage.getOnlineUsers().size()-1)+".jpg",storage.getFromUsername(),true);
//            members.add(member);
            memberMaps.put(storage.getFromUsername(),member);
            addMember(member);

        }
        // 下线
        else if(messageType == CONSTANT.MessageType.OFFLINE_MESSAGE.getCode()){
            log.info(storage.getFromUsername()+"离开聊天室了呜QAQ");
            addCommonMsg(storage.getFromUsername()+"离开聊天室了呜QAQ");
            removeMember(storage.getFromUsername());

        }
        // 私信消息
        else if(messageType==CONSTANT.MessageType.PRIVATE_MESSAGE.getCode()){
            log.info("收到来自【"+storage.getFromUsername()+"】的私信消息："+storage.getMessage());
            // 1如果刚好在发送方聊天界面，不进行界面切换
            if (curChatUser.equals(storage.getFromUsername())){
                Message receiveMsg = new Message(storage.getFromUsername(),storage.getMessage(),new Timestamp(new Date().getTime()));
                addMessageBox(receiveMsg);
                return;
            }
            // 2当前不在好友页面，先添加小红点提醒，再获取对方的message列表再填充

            // a 添加小红点
            // a.1.获取对方的卡片
            HBox receiverCard=null;
            for (Node n:memberCardMaps.keySet()){
                if (memberCardMaps.get(n).equals(storage.getFromUsername())){
                    receiverCard = (HBox) n;
                }
            }
            // a.2 添加小红点
            Circle circle = new Circle(5);
            circle.setFill(Color.rgb(255,0,0));
            circle.setLayoutX(10);
            circle.setLayoutY(10);
            AnchorPane anchorPane = (AnchorPane) receiverCard.getChildren().get(0);
            try {
                Platform.runLater(()->{anchorPane.getChildren().add(1,circle);});
            }catch (Exception e){
                log.info("已经有小红点咯，不要重复添加啦");
            }

            // 2.1.消息列表存在
            if (userPanel.get(storage.getFromUsername())!= null){
                Message receiveMsg = new Message(storage.getFromUsername(),storage.getMessage(),new Timestamp(new Date().getTime()));
                userPanel.get(storage.getFromUsername()).add(getMessageBox(receiveMsg));// 增加消息



                return;
            }
            // 2.2.消息列表不存在
            Message receiveMsg = new Message(storage.getFromUsername(),storage.getMessage(),new Timestamp(new Date().getTime()));
            Collection<Node> nodes = new ArrayList<>();
            nodes.add(getMessageBox(receiveMsg));
            userPanel.put(storage.getFromUsername(),nodes);
        }
        // 群发消息
        else if(messageType==CONSTANT.MessageType.PUBLIC_MESSAGE.getCode()){
            log.info("收到来自【"+storage.getFromUsername()+"】的群发消息："+storage.getMessage());
            // 1.如果是自己的消息，不用接收再次渲染一次咯
            if (storage.getFromUsername().equals(storage.getLoginUser()))
                return;

            Message receiveMsg = new Message(storage.getFromUsername(),storage.getMessage(),new Timestamp(new Date().getTime()));
            // 2.如果当前页面不是群聊界面，先添加小红点提醒，则将消息保存到群聊中
            if (!curChatUser.equals("群聊")){

                // a 小红点
                // a.1.获取群聊卡片
                HBox receiverCard= (HBox) membersList.getChildren().get(0);

                // a.2 添加小红点
                Circle circle = new Circle(5);
                circle.setFill(Color.rgb(255,0,0));
                circle.setLayoutX(10);
                circle.setLayoutY(10);
                AnchorPane anchorPane = (AnchorPane) receiverCard.getChildren().get(0);
                try {
                    Platform.runLater(()->{anchorPane.getChildren().add(1,circle);});
                }catch (Exception e){
                    log.info("已经有小红点咯，不要重复添加啦");
                }

                userPanel.get("群聊").add(getMessageBox(receiveMsg));
                return;
            }
            // 3.对方的消息，进行渲染
            addMessageBox(receiveMsg);
        }
    }
    /**
     * 处理语音消息接收
     */
    public void handleAudioMessage(){
        // 1.填充数据
        AudioMsg audioMsg = new AudioMsg();
        audioMsg.setId(storage.getAudioHistoryList().size()-1);
        audioMsg.setSenderName(storage.getAudioSender());
        audioMsg.setAudio(storage.getAudioByteArr());
        Node audioBox = getAudioMsgBox(audioMsg);

        // 1.1 群发语音 并 渲染
        if (storage.getAudioReceiver().isEmpty()){
            if (curChatUser.equals("群聊")) addAudioMsgBox(audioMsg);//群聊且刚好在群聊界面，直接渲染
            else {// 不在群聊界面的，先持久化保存
                userPanel.get("群聊").add(audioBox);

                // a 小红点
                // a.1.获取群聊卡片
                HBox receiverCard= (HBox) membersList.getChildren().get(0);
                // a.2 添加小红点
                Circle circle = new Circle(5);
                circle.setFill(Color.rgb(255,0,0));
                circle.setLayoutX(10);
                circle.setLayoutY(10);
                AnchorPane anchorPane = (AnchorPane) receiverCard.getChildren().get(0);
                try {
                    Platform.runLater(()->{anchorPane.getChildren().add(1,circle);});
                }catch (Exception e){
                    log.info("已经有小红点咯，不要重复添加啦");
                }
            }

            return;
        }
        // 1.2 私聊语音
        else {
            // panel已经存在
            if(userPanel.get(storage.getAudioSender()) != null){
                if (curChatUser.equals(storage.getAudioSender())){// 私聊且在私聊界面
                    addAudioMsgBox(audioMsg);
                    return;
                }// 私聊但在私聊界面，存储即可
                userPanel.get(storage.getAudioSender()).add(audioBox);
                return;
            }
            // panel 不存在
            Collection<Node> nodes = new ArrayList<>();
            nodes.add(audioBox);
            userPanel.put(storage.getAudioSender(),nodes);

            // a 添加小红点
            // a.1.获取对方的卡片
            HBox receiverCard=null;
            for (Node n:memberCardMaps.keySet()){
                if (memberCardMaps.get(n).equals(storage.getAudioSender())){
                    receiverCard = (HBox) n;
                }
            }
            // a.2 添加小红点
            Circle circle = new Circle(5);
            circle.setFill(Color.rgb(255,0,0));
            circle.setLayoutX(10);
            circle.setLayoutY(10);
            AnchorPane anchorPane = (AnchorPane) receiverCard.getChildren().get(0);
            try {
                Platform.runLater(()->{anchorPane.getChildren().add(1,circle);});
            }catch (Exception e){
                log.info("已经有小红点咯，不要重复添加啦");
            }
        }

    }

    /**
     * 处理语音消息发送
     */
    public void record(){
        if (recordBtn.getText().equals("录音")){
            audioController.capture();
            storage.setAudioReceiver(curChatUser.equals("群聊")?"":curChatUser);
            Platform.runLater(()->{
                recordImg.setImage(new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/speaking.gif"));
            });
            recordBtn.setText("停止");
        }else if (recordBtn.getText().equals("停止")){
            audioController.stop();

            // 把自己的消息渲染在聊天框内
            AudioMsg audioMsg = new AudioMsg();
            audioMsg.setId(storage.getAudioHistoryList().size()-1);
            audioMsg.setSenderName(storage.getLoginUser());
            audioMsg.setAudio(storage.getAudioByteArr());
            addAudioMsgBox(audioMsg);
            Platform.runLater(()->{
                recordImg.setImage(new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/speak.png"));
            });
            recordBtn.setText("录音");
            //发送二进制语音消息

        }
    }

    /**
     * 渲染语音消息盒子
     */
    private void addAudioMsgBox(AudioMsg message) {
        Member sender = getMemberByName(message.getSenderName());
        assert sender != null;
//        Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(sender.getAvatar())));
        Image headImg = new Image(sender.getAvatar());
        ImageView head = new ImageView();
        Platform.runLater(()->{
            head.setImage(headImg);
        });
        head.setFitWidth(40);
        head.setFitHeight(40);

        Label messageBubble = new Label();
        messageBubble.setPrefWidth(50);
        messageBubble.setWrapText(true);
        messageBubble.setMaxWidth(220);
        messageBubble.setPadding(new Insets(6));
        messageBubble.setFont(new Font(14));
        HBox.setMargin(messageBubble, new Insets(8, 0, 0, 0));

        boolean isMine = message.getSenderName().equals(storage.getLoginUser());
        double[] points;
        if (!isMine) {
            points = new double[]{
                    0.0, 5.0,
                    10.0, 0.0,
                    10.0, 10.0
            };
        } else {
            points = new double[]{
                    0.0, 0.0,
                    0.0, 10.0,
                    10.0, 5.0
            };
        }
        Polygon triangle = new Polygon(points);
        HBox messageBox = new HBox();
        messageBox.setPrefWidth(366);
        messageBox.setPadding(new Insets(15, 5, 10, 5));
        if (isMine) {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(179,231,244); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(179,231,244));

            HBox.setMargin(triangle, new Insets(15, 10, 0, 0));
            // 添加user的布局位置
            Text username = new Text(storage.getLoginUser());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));

            // 将triangle和messageBubble放在Hbox内，一起放在一个锚布局内，并添加image播放
            HBox msgBubbleCan = new HBox();
            msgBubbleCan.getChildren().addAll(messageBubble,triangle);
            // 暂停部分
            Image wifiStop = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_r_stop.png");
            ImageView wifiPng = new ImageView();
            Platform.runLater(()->{
                wifiPng.setImage(wifiStop);
            });
            wifiPng.setFitHeight(20);
            wifiPng.setFitWidth(20);
            wifiPng.setLayoutX(15);
            wifiPng.setLayoutY(14);
            // 播放部分
            Image wifiStart = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_r_start.gif");
            ImageView wifiGif = new ImageView();
            Platform.runLater(()->{
                wifiGif.setImage(wifiStart);
            });
            wifiGif.setFitHeight(20);
            wifiGif.setFitWidth(20);
            wifiGif.setLayoutX(15);
            wifiGif.setLayoutY(14);
            // 设置可见性
            wifiGif.setVisible(false);

            AnchorPane msgCard = new AnchorPane();
            HBox.setMargin(msgCard,new Insets(0,0,0,-5));
            msgCard.getChildren().addAll(msgBubbleCan,wifiGif,wifiPng);

            Platform.runLater(()->{ messageBox.getChildren().addAll(msgCard, userInfo);  });

            messageBox.setAlignment(Pos.TOP_RIGHT);
        } else {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(255,255,255); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(255,255,255));

            HBox.setMargin(triangle, new Insets(15, 0, 0, 10));
            // 添加user的布局位置
            Text username = new Text(sender.getName());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));

            // 将triangle和messageBubble放在Hbox内，一起放在一个锚布局内，并添加image播放(暂停）【主要是添加事件还要判断左右，懒得再判断了直接放两个然后改可见】
            HBox msgBubbleCan = new HBox();
            msgBubbleCan.getChildren().addAll(triangle,messageBubble);
            // 播放部分
            Image wifiStart = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_l_start.gif");
            ImageView wifiGif = new ImageView(wifiStart);
            Platform.runLater(()->{
                wifiGif.setImage(wifiStart);
            });
            wifiGif.setFitHeight(20);
            wifiGif.setFitWidth(20);
            wifiGif.setLayoutX(33);
            wifiGif.setLayoutY(14);
            // 暂停部分
            Image wifiStop = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_l_stop.png");
            ImageView wifiPng = new ImageView(wifiStop);
            Platform.runLater(()->{
                wifiPng.setImage(wifiStop);
            });
            wifiPng.setFitHeight(20);
            wifiPng.setFitWidth(20);
            wifiPng.setLayoutX(33);
            wifiPng.setLayoutY(14);
            // 设置可见性
            wifiGif.setVisible(false);

            AnchorPane msgCard = new AnchorPane();
            HBox.setMargin(msgCard,new Insets(0,0,0,-5));
            msgCard.getChildren().addAll(msgBubbleCan,wifiGif,wifiPng);

            Platform.runLater(()->{ messageBox.getChildren().addAll(userInfo, msgCard);  });
        }
        last = scrollPane.getVvalue() == 1.0;
        Platform.runLater(()->{
            messagesList.getChildren().add(messageBox);
        });
        audioMsgMaps.put(messageBox,message.getId());

        // 为AudioMessageBox添加点击播放事件
        EventHandler<MouseEvent> handler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (storage.getIsClick())//防止重复点击
                    return;
                // 获取消息气泡内的Image并改为动图
                HBox h = (HBox) event.getSource();
                AnchorPane msgCard=null;
                try {
                    msgCard = (AnchorPane) h.getChildren().get(1);
                }catch (Exception e){
                    msgCard = (AnchorPane) h.getChildren().get(0);

                }
                ImageView wifiGif = (ImageView) msgCard.getChildren().get(1);
                ImageView wifiPng = (ImageView) msgCard.getChildren().get(2);
                Platform.runLater(()->{
                    wifiPng.setVisible(false);
                    wifiGif.setVisible(true);
                });

                // 存储该卡片，方便音乐停止调整gif
                storage.setCurPlayPane(msgCard);

                // 根据点击找到对应的消息条，并播放消息
                int msgId = audioMsgMaps.get(h);
                String fromUser = storage.getAudioHistoryList().get(msgId);
                log.info("用户"+fromUser+"发送的音频消息~");
                audioController.play(storage.getAudioMap().get(msgId));

            }
        };
        messageBox.addEventFilter(MouseEvent.MOUSE_CLICKED,handler);
    }
    /**
     * 语音消息盒子加工厂（包装）
     */
    private Node getAudioMsgBox(AudioMsg message) {
        Member sender = getMemberByName(message.getSenderName());
        assert sender != null;
//        Image headImg = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(sender.getAvatar())));
        Image headImg = new Image(sender.getAvatar());
        ImageView head = new ImageView();
        Platform.runLater(()->{
            head.setImage(headImg);
        });
        head.setFitWidth(40);
        head.setFitHeight(40);

        Label messageBubble = new Label();
        messageBubble.setPrefWidth(50);
        messageBubble.setWrapText(true);
        messageBubble.setMaxWidth(220);
        messageBubble.setPadding(new Insets(6));
        messageBubble.setFont(new Font(14));
        HBox.setMargin(messageBubble, new Insets(8, 0, 0, 0));

        boolean isMine = message.getSenderName().equals(storage.getLoginUser());
        double[] points;
        if (!isMine) {
            points = new double[]{
                    0.0, 5.0,
                    10.0, 0.0,
                    10.0, 10.0
            };
        } else {
            points = new double[]{
                    0.0, 0.0,
                    0.0, 10.0,
                    10.0, 5.0
            };
        }
        Polygon triangle = new Polygon(points);
        HBox messageBox = new HBox();
        messageBox.setPrefWidth(366);
        messageBox.setPadding(new Insets(15, 5, 10, 5));
        if (isMine) {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(179,231,244); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(179,231,244));

            HBox.setMargin(triangle, new Insets(15, 10, 0, 0));
            // 添加user的布局位置
            Text username = new Text(storage.getLoginUser());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));

            // 将triangle和messageBubble放在Hbox内，一起放在一个锚布局内，并添加image播放
            HBox msgBubbleCan = new HBox();
            msgBubbleCan.getChildren().addAll(messageBubble,triangle);
            // 暂停部分
            Image wifiStop = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_r_stop.png");
            ImageView wifiPng = new ImageView();
            Platform.runLater(()->{
                wifiPng.setImage(wifiStop);
            });
            wifiPng.setFitHeight(20);
            wifiPng.setFitWidth(20);
            wifiPng.setLayoutX(15);
            wifiPng.setLayoutY(14);
            // 播放部分
            Image wifiStart = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_r_start.gif");
            ImageView wifiGif = new ImageView();
            Platform.runLater(()->{
                wifiGif.setImage(wifiStart);
            });
            wifiGif.setFitHeight(20);
            wifiGif.setFitWidth(20);
            wifiGif.setLayoutX(15);
            wifiGif.setLayoutY(14);
            // 设置可见性
            wifiGif.setVisible(false);

            AnchorPane msgCard = new AnchorPane();
            HBox.setMargin(msgCard,new Insets(0,0,0,-5));
            msgCard.getChildren().addAll(msgBubbleCan,wifiGif,wifiPng);

            Platform.runLater(()->{ messageBox.getChildren().addAll(msgCard, userInfo);  });

            messageBox.setAlignment(Pos.TOP_RIGHT);
        } else {
            // 设置颜色
            messageBubble.setStyle("-fx-background-color: rgb(255,255,255); -fx-background-radius: 8px;");
            triangle.setFill(Color.rgb(255,255,255));

            HBox.setMargin(triangle, new Insets(15, 0, 0, 10));
            // 添加user的布局位置
            Text username = new Text(sender.getName());
            VBox userInfo = new VBox(username,head);
            HBox.setMargin(userInfo,new Insets(-15,0,0,0));

            // 将triangle和messageBubble放在Hbox内，一起放在一个锚布局内，并添加image播放(暂停）【主要是添加事件还要判断左右，懒得再判断了直接放两个然后改可见】
            HBox msgBubbleCan = new HBox();
            msgBubbleCan.getChildren().addAll(triangle,messageBubble);
            // 暂停部分
            Image wifiStop = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_l_stop.png");
            ImageView wifiPng = new ImageView(wifiStop);
            Platform.runLater(()->{
                wifiPng.setImage(wifiStop);
            });
            wifiPng.setFitHeight(20);
            wifiPng.setFitWidth(20);
            wifiPng.setLayoutX(33);
            wifiPng.setLayoutY(14);
            // 播放部分
            Image wifiStart = new Image("https://orange-1312206514.cos.ap-guangzhou.myqcloud.com/image/wifi_l_start.gif");
            ImageView wifiGif = new ImageView(wifiStart);
            Platform.runLater(()->{
                wifiPng.setImage(wifiStop);
            });
            wifiGif.setFitHeight(20);
            wifiGif.setFitWidth(20);
            wifiGif.setLayoutX(33);
            wifiGif.setLayoutY(14);
            // 设置可见性
            wifiGif.setVisible(false);

            AnchorPane msgCard = new AnchorPane();
            HBox.setMargin(msgCard,new Insets(0,0,0,-5));
            msgCard.getChildren().addAll(msgBubbleCan,wifiPng,wifiGif);

            Platform.runLater(()->{ messageBox.getChildren().addAll(userInfo, msgCard);  });
        }
        last = scrollPane.getVvalue() == 1.0;

        // 为AudioMessageBox添加点击播放事件
        EventHandler<MouseEvent> handler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (storage.getIsClick())//防止重复点击
                    return;
                // 获取消息气泡内的Image并改为动图
                HBox h = (HBox) event.getSource();
                AnchorPane msgCard = (AnchorPane) h.getChildren().get(1);
                ImageView wifiGif = (ImageView) msgCard.getChildren().get(1);
                ImageView wifiPng = (ImageView) msgCard.getChildren().get(2);
                Platform.runLater(()->{
                    wifiPng.setVisible(false);
                    wifiGif.setVisible(true);
                });

                // 存储该卡片，方便音乐停止调整gif
                storage.setCurPlayPane(msgCard);

                // 根据点击找到对应的消息条，并播放消息
                int msgId = audioMsgMaps.get(h);
                String fromUser = storage.getAudioHistoryList().get(msgId);
                log.info("用户"+fromUser+"发送的音频消息~");
                audioController.play(storage.getAudioMap().get(msgId));
            }
        };
        messageBox.addEventFilter(MouseEvent.MOUSE_CLICKED,handler);
//        Platform.runLater(()->{
//            messagesList.getChildren().add(messageBox);
//        });
        audioMsgMaps.put(messageBox,message.getId());
        return messageBox;
    }
}
