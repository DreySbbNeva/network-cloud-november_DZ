import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
@Slf4j // Это значит,когда код данного класса начнет компилироваться,то компилятор обратится к движку ломбока и он нагенерит код из файла в ресурсах log4j.properties.
public class Server {

    public static void main(String[] args) {

        try {
            try (ServerSocket server = new ServerSocket(8189)) {
                log.debug("Server started...");
                while (true) {
                    Socket socket = server.accept();
                    log.debug("Client accepted");
                    Handler handler = new Handler(socket);
                    new Thread(handler).start();
                }
            }
        } catch (IOException e) {
            log.error("",e);
        }
    }
}
