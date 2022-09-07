package org.csu.chat.controller;


import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;
import org.csu.chat.utils.WSClient;
import org.csu.chat.utils.WSStorage;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class AudioController {

    //定义录音格式
    AudioFormat af = null;
    //定义目标数据行,可以从中读取音频数据,该 TargetDataLine 接口提供从目标数据行的缓冲区读取所捕获数据的方法。
    TargetDataLine td = null;
    //定义源数据行,源数据行是可以写入数据的数据行。它充当其混频器的源。应用程序将音频字节写入源数据行，这样可处理字节缓冲并将它们传递给混频器。
    SourceDataLine sd = null;
    //定义字节数组输入输出流
    ByteArrayInputStream bais = null;
    ByteArrayOutputStream baos = null;
    byte audioData[];//输出字节流
    //定义音频输入流
    AudioInputStream ais = null;
    //定义停止录音的标志，来控制录音线程的运行
    Boolean stopflag = false;

    private WSClient webSocketClient;
    private WSStorage wsStorage;

    private List<String> userRecordList;// 按顺序发过语音的人的列表名 比如第一条语音 1：orange
    private HashMap<String,Byte[]> recodeMap;// 记录id对应的语音 比如第一条orange的语音 1：[二进制语音数据]

    public AudioController() {
        String loginUsername = WSStorage.getInstance().getLoginUser();
        webSocketClient = WSClient.getInstance();
        wsStorage = WSStorage.getInstance();

        userRecordList = new ArrayList<>();
        recodeMap = new HashMap<>();
    }


    //开始录音
    public void capture() {
        try {
            //af为AudioFormat也就是音频格式
            if (af != null)
                af = null;
            af = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, af);
            td = (TargetDataLine) (AudioSystem.getLine(info));
            //打开具有指定格式的行，这样可使行获得所有所需的系统资源并变得可操作。
            td.open(af);
            //允许某一数据行执行数据 I/O
            td.start();
            //创建播放录音的线程
            Record record = new Record();
            Thread t1 = new Thread(record);
            t1.start();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }
    }
    //停止录音
    public void stop() {
        td.stop();
        stopflag = true;

        if (baos == null){
            log.info("录制时间太短，导致未接收到二进制消息，请重试！");
            // TODO 弹窗提醒用户
            return;
        }

        try {
            String username = wsStorage.getAudioReceiver();// 发送语音接收方 如果是群聊，则为空
            byte[] userPayload=null;
//            if (!username.isEmpty()){// 私信语音
            if (username!=null){// 私信语音
                //储存二进制流数据,封装成20字节username信息+二进制流信息
                userPayload = new byte[20];
                int i=0;
                for (byte item:username.getBytes()) {
                    userPayload[i++]=item;
                }
                ByteBuffer byteBuffer = ByteBuffer.allocate(20+baos.size());
                byteBuffer.put(userPayload);
                byteBuffer.put(baos.toByteArray());
                webSocketClient.send(byteBuffer.array());// 发送包装后的二进制流数据
            }else {// 群发语音
                webSocketClient.send(baos.toByteArray());// 直接发送二进制流数据
            }

            // 储存记录自己的录音信息
            wsStorage.setAudioSender(wsStorage.getLoginUser());
            wsStorage.setAudioByteArr(baos.toByteArray());
            wsStorage.getAudioHistoryList().add(wsStorage.getLoginUser());
            wsStorage.getAudioMap().put(wsStorage.getAudioHistoryList().size()-1,baos.toByteArray());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    //播放录音
    public void play(byte[] audioDataByte) {
        //将baos中的数据转换为字节数据
//        byte audioData[] = baos.toByteArray();
//        audioData = baos.toByteArray();
        //转换为输入流
        bais = new ByteArrayInputStream(audioDataByte);
        af = getAudioFormat();
        ais = new AudioInputStream(bais, af, audioDataByte.length / af.getFrameSize());
        try {
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, af);
            sd = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sd.open(af);
            sd.start();
            //创建播放进程
            Play py = new Play();
            Thread t2 = new Thread(py);
            t2.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭流
                if (ais != null) {
                }
                if (bais != null) {
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //设置AudioFormat的参数
    public AudioFormat getAudioFormat() {
        //下面注释部分是另外一种音频格式，两者都可以
        AudioFormat.Encoding encoding = AudioFormat.Encoding.
                PCM_SIGNED;
        float rate = 8000f;
        int sampleSize = 16;
        String signedString = "signed";
        boolean bigEndian = true;
        int channels = 1;
        return new AudioFormat(encoding, rate, sampleSize, channels,
                (sampleSize / 8) * channels, rate, bigEndian);
//		//采样率是每秒播放和录制的样本数
//		float sampleRate = 16000.0F;
//		// 采样率8000,11025,16000,22050,44100
//		//sampleSizeInBits表示每个具有此格式的声音样本中的位数
//		int sampleSizeInBits = 16;
//		// 8,16
//		int channels = 1;
//		// 单声道为1，立体声为2
//		boolean signed = true;
//		// true,false
//		boolean bigEndian = true;
//		// true,false
//		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,bigEndian);
    }


    //录音类，因为要用到MyRecord类中的变量，所以将其做成内部类
    class Record implements Runnable {
        //定义存放录音的字节数组,作为缓冲区
        byte bts[] = new byte[10000];
        //将字节数组包装到流里，最终存入到baos中
        //重写run函数
        @Override
        public void run() {
            baos = new ByteArrayOutputStream();
            try {
                System.out.println("ok3");
                stopflag = false;
                while (stopflag != true) {
                    //当停止录音没按下时，该线程一直执行
                    //从数据行的输入缓冲区读取音频数据。
                    //要读取bts.length长度的字节,cnt 是实际读取的字节数
                    int cnt = td.read(bts, 0, bts.length);
                    if (cnt > 0) {
                        baos.write(bts, 0, cnt);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    //关闭打开的字节数组流
                    if (baos != null) {
                        baos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    td.drain();
                    td.close();
                }
            }
        }

    }

    //播放类,同样也做成内部类
    class Play implements Runnable {
        //播放baos中的数据即可
        @Override
        public void run() {
            byte bts[] = new byte[10000];
            try {
                int cnt;
                //读取数据到缓存数据
                while ((cnt = ais.read(bts, 0, bts.length)) != -1) {
                    if (cnt > 0) {
                        //写入缓存数据
                        //将音频数据写入到混频器
                        sd.write(bts, 0, cnt);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                sd.drain();
                sd.close();
                // 播放结束后wifi变为静态
                AnchorPane msgCard = wsStorage.getCurPlayPane();
                ImageView wifiGif = (ImageView) msgCard.getChildren().get(1);
                ImageView wifiPng = (ImageView) msgCard.getChildren().get(2);
                Platform.runLater(()->{wifiPng.setVisible(true);wifiGif.setVisible(false);});
                wsStorage.setIsClick(false);// 恢复点击状态
                System.out.println("音乐停止咯-------------------------");
            }
        }
    }


    public void handleWSMessage(){
//        audioData = wsStorage.getAudioByteArr(); 这个可以实时语音的想法上靠拢
//        play();//一收到消息就播放（TODO 可能用作实时聊天，暂且不用，我们先做语音条）
    }
}
