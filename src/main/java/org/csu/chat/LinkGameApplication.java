package org.csu.chat;

import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import org.csu.chat.view.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LinkGameApplication extends AbstractJavaFxApplicationSupport {
    public static void main(String[] args) {
        launch(LinkGameApplication.class, ChatView.class, args);
    }

}
