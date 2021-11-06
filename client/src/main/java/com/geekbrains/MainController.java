package com.geekbrains;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;
    public TextField input;
    public TextField TextFieldLeft;
    public TextField TextFieldRight;
    private DataInputStream in;
    private DataOutputStream out;
    private Path clientDir;
    private String itemLeft = null;
    private String itemRight = null;
    private boolean flagList = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            clientDir = Paths.get("client", "clientFile");

            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }
            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(clientDir));
            clientView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    itemLeft = clientView.getSelectionModel().getSelectedItem();
                    TextFieldLeft.setText(itemLeft);
                }
            });
            serverView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    itemRight = serverView.getSelectionModel().getSelectedItem();
                    TextFieldRight.setText(itemRight);
                }
            });
            Socket socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            // Делает наш запрос сервисным, то есть поток будет закрыт тогда когда будет закрыт поток main. Иначе если поток оставить *обычным* не закрыть, то поток продолжит свою работу.
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void read() {
        try {
            while (true) {
// Получение листа файлов с сервера
                while (flagList) {
                    String msg = in.readUTF();
                    if (msg.equals("*!*")) {
                        flagList = false;
                        break;
                    }
                    log.debug("Полученно сообщение {} ", msg);
                    Platform.runLater(() -> {
                        serverView.getItems().add(msg);
                    });
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private List<String> getFiles(Path path) throws IOException {
        return Files.list(path).map(p -> p.getFileName().toString()).collect(Collectors.toList());
    }

    public void sendMessage(ActionEvent actionEvent) throws IOException {
        String text = input.getText();
        out.writeUTF(text);
        out.flush();
        input.clear();
    }

    public void sendClearLeft() {
        TextFieldLeft.clear();
    }

    public void sendClearRight() {
        TextFieldRight.clear();
    }

    public void sendFileToServer() {
        if (itemLeft != null) {
            try {
                out.writeUTF("| " + itemLeft);
                Path clientRootFile = Paths.get("client", "clientFile", itemLeft);
                try (FileInputStream fileInputStream = new FileInputStream(clientRootFile.toString());) {
                    byte[] buffer = new byte[256];
                    int read;
                    while ((read = fileInputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.close();
                }
            } catch (IOException e) {
                log.error("Ошибка при отправке файла от клиента !!!");
            }
        }
    }


    public void sendFileFromServer(ActionEvent actionEvent) {
        if (itemRight != null) {
            try {
                out.writeUTF("/ " + itemRight);
                Path servertRootFile = Paths.get("client", "clientFile", itemRight);
                if (!Files.exists(servertRootFile)) {
                    Files.createFile(servertRootFile);
                }
                byte[] buffer = new byte[256];
                int read;
                try (FileOutputStream fileOutputStream = new FileOutputStream(servertRootFile.toFile())) {
                    while ((read = in.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                    in.close();
                    clientView.getItems().clear();
                    clientView.getItems().addAll(getFiles(clientDir));
                }
            } catch (IOException e) {
                log.error("Ошибка при отправке файла от клиента !!!");
            }
        }
    }
}