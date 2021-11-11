package com.geekbrains;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

@Slf4j
public class ConsolEx {
    private ByteBuffer buffer;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final Path ROOT = Paths.get("/Users/aiiganty/IdeaProjects/network-cloud-november/client");

    public ConsolEx() {
        buffer = ByteBuffer.allocate(8192);
        try {
            // Каналы сокетов сервера безопасны для использования несколькими параллельными потоками.
            serverChannel = ServerSocketChannel.open(); // создаем серверный канал
            /* Канал сокета сервера создается путем вызова метода open этого класса. Невозможно создать канал для произвольного, ранее существовавшего сервера.
            Недавно созданный канал сокета сервера открыт, но еще не привязан. Попытка вызвать метод accept несвязанного канала сокета сервера приведет к возникновению исключения NotYetBoundException.
            Канал сокета сервера может быть привязан путем вызова одного из методов привязки, определенных этим классом. */
            serverChannel.bind(new InetSocketAddress(8189)); // Привязывает сокет канала к локальному адресу и настраивает сокет для прослушивания соединений.
            serverChannel.configureBlocking(false); // Выставляем асинхронный режим.
            selector = Selector.open(); // Создаем селектор
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            // Регистрируем серверный канал на селекторе и говорим ему(селетору) слушай события accept
            /*Регистрация выбираемого канала с помощью селектора представлена объектом SelectionKey. Селектор поддерживает три набора клавиш выбора:

Набор ключей содержит ключи, представляющие текущие регистрации каналов этого селектора. Этот набор возвращается методом ключей.

Набор выбранных ключей - это набор ключей, такой, чтобы канал каждого ключа был обнаружен готовым по меньшей мере к одной из операций, определенных в наборе интересов ключа во время предыдущей операции выбора.
Этот набор возвращается методом selectedKeys. Набор выбранных ключей всегда является подмножеством набора ключей.

Набор отмененных ключей - это набор ключей, которые были отменены, но чьи каналы еще не были сняты с регистрации. Этот набор недоступен напрямую. Набор отмененных ключей всегда является подмножеством набора ключей.*/
            log.debug("Server start");
            while (serverChannel.isOpen()) {
                selector.select();
                // Выбирает набор ключей, соответствующие каналы которых готовы к операциям ввода-вывода.
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // Ключ на активациию
                    if (key.isAcceptable()) {
                        handleAccept();
                    }
                    // Ключ на чтение запись.
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    selectionKeys.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
        Boolean flag = false;
        while(true){
            System.out.println("Begin");
            int read = channel.read(buffer);
            if (read == -1) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            if (read > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    sb.append((char) buffer.get());
                }
                buffer.clear();
                flag=true;
            }

            while (flag){
                if (sb.toString().startsWith("ls-")) {
                    if (sb.toString().length() == 5) {
                        Files.walkFileTree(ROOT, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                channel.write(ByteBuffer.wrap((file.toString() + "\n").getBytes(StandardCharsets.UTF_8)));
                                return super.visitFile(file, attrs);
                            }
                        });
                        sb = new StringBuilder();
                        flag=false;
                        break;
                    }

                    String str = sb.toString().split("-", 0)[1];
                    Path path = Paths.get("/Users/aiiganty/IdeaProjects/network-cloud-november/client", str);
                    System.out.println(path);
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            channel.write(ByteBuffer.wrap((file.toString() + "\n").getBytes(StandardCharsets.UTF_8)));
                            return super.visitFile(file, attrs);
                        }
                    });
                    sb = new StringBuilder();
                    flag=false;
                    break;
                }

                if (sb.toString().trim().startsWith("cat-")){
                    String str1 = sb.toString().trim();
                    int indexMax1 = str1.length();
                    int indexMin1 = 4;
                    String filePath = str1.substring(indexMin1,indexMax1);
                    Path path = Paths.get("./client/",filePath);
                    if (Files.exists(path)){
                        List<String> lines = null;
                        try {
                            lines = Files.readAllLines(path);
                            for (String str : lines){
                                channel.write(ByteBuffer.wrap((str).getBytes(StandardCharsets.UTF_8)));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else {
                        channel.write(ByteBuffer.wrap(("Файл не найден!!!").getBytes(StandardCharsets.UTF_8)));
                    }
                    flag=false;
                    sb = new StringBuilder();
                    break;
                }
            }
            System.out.println("END");
        }
    }

    private void handleAccept () throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        log.debug("Client Connected!!!");
    }



    public static void main(String[] args) {
        new ConsolEx();
    }
}

