import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Handler implements Runnable {

    private static int counter = 0;

    private final DataOutputStream out;
    private final DataInputStream in;
    private final String name;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Path serverDir;


    public Handler(Socket socket) throws IOException {
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        counter++;
        name = "User#" + counter;
        log.debug("Set nick: {} for new client, name", name); // {} - эсслоуфуджи - это как принт формате %s, толькоможно передать любой Object
        serverDir = Paths.get("server", "serverFile");
        List<String> list = Files.list(serverDir).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        for (String msg : list) {
            out.writeUTF(msg);
        }
        out.writeUTF("*!*");
    }

    private String getDate() {
        return formatter.format(LocalDateTime.now());
    }

    @Override
    public void run() {
        try {

            while (true) {
                String msg = in.readUTF();
                String[] splitArray = msg.split(" ");

                if (splitArray[0].equals("]")) {
                    List<String> list = Files.list(serverDir).map(p -> p.getFileName().toString()).collect(Collectors.toList());
                    for (String str : list) {
                        out.writeUTF(str);
                    }
                    out.writeUTF("*!*");
                }
                // Передача с клиента на сервер
                if (splitArray[0].equals("|")) {
                    Path serverFile = Paths.get("server", "serverFile", splitArray[1]);
                    if (!Files.exists(serverFile)) {
                        Files.createFile(serverFile);
                    }
                    int counterBytes;
                    byte[] buffer = new byte[256];
                    try (FileOutputStream fileOutputStream = new FileOutputStream(serverFile.toFile())) {
                        while ((counterBytes = in.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, counterBytes);
                        }
                        in.close();
                    }
                }
                // Передача файла с сервера на клиент.
                if (splitArray[0].equals("/")) {
                    Path serverFile = Paths.get("server", "serverFile", splitArray[1]);
                    if (!Files.exists(serverFile)) {
                        log.error("Запрошенный файл не существует!!!");
                        continue;
                    }
                    byte[] buffer = new byte[256];
                    int read;
                        try (FileInputStream fileInputStream = new FileInputStream(serverFile.toString());) {
                            while ((read = fileInputStream.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.close();
                        }
                    }
                }
            }catch (IOException e) {
            log.error("", e);
        }
    }
}