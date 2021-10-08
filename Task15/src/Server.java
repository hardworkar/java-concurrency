// https://docs.oracle.com/en/java/javase/16/core/non-blocking-time-server-nio-example.html
// https://www.programcreek.com/java-api-examples/?class=java.nio.channels.SelectionKey&method=attach
// https://javatutor.net/articles/working-with-selectors
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private final Selector selector = Selector.open();
    private final String hostTranslateTo;
    private final int portNumberTranslateTo;
    private int connections = 0;
    public Server(final int port, String hostTranslateTo, int portNumberTranslateTo) throws Exception {
        this.hostTranslateTo = hostTranslateTo;
        this.portNumberTranslateTo = portNumberTranslateTo;
        acceptConnections(port);
    }
    class Translator{
        private final SocketChannel socketChannel;
        private final ByteBuffer readFrom, writeTo;
        private final SelectionKey key;
        private Translator otherTranslator;
        public Translator(SelectionKey key, Translator otherTranslator, ByteBuffer readFrom, ByteBuffer writeTo){
            this.socketChannel = (SocketChannel) key.channel();
            this.readFrom = readFrom;
            this.writeTo = writeTo;
            this.key = key;
            this.otherTranslator = otherTranslator;
        }
        public boolean write() throws IOException {
            writeTo.flip();
            socketChannel.write(writeTo);
            return writeTo.remaining() == 0;
        }
        public void read() throws IOException {
            readFrom.clear();
            if(socketChannel.read(readFrom) == -1){
                key.channel().close();
                key.cancel();
                otherTranslator.key.channel().close();
                otherTranslator.key.cancel();
                connections--;
                System.out.println("One connection closed, now: " + connections + " connections");
                return;
            }
            if(!otherTranslator.write()) {
                System.out.println("Не получилось нормально записать :(");
                otherTranslator.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }
    }

    private void doAccept(SelectionKey sk){
        ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();
        SocketChannel channel;
        try {
            channel = nextReady.accept();
            System.out.println("Connection from: " + channel.socket());

            channel.configureBlocking(false);
            SelectionKey clientKey = channel.register(selector, SelectionKey.OP_READ);

            InetSocketAddress isa = new InetSocketAddress(hostTranslateTo, portNumberTranslateTo);
            SocketChannel clientToServerCon = SocketChannel.open(isa);

            clientToServerCon.configureBlocking(false);
            SelectionKey serverKey = clientToServerCon.register(selector, SelectionKey.OP_READ);


            ByteBuffer toClientBuffer = ByteBuffer.allocate(1024);
            ByteBuffer fromClientBuffer = ByteBuffer.allocate(1024);

            Translator serverTrans = new Translator(serverKey, null, fromClientBuffer, toClientBuffer);
            Translator clientTrans = new Translator(clientKey, serverTrans, toClientBuffer, fromClientBuffer);
            serverTrans.otherTranslator = clientTrans;

            serverKey.attach(serverTrans);
            clientKey.attach(clientTrans);

            connections++;
            System.out.println("Got acceptable key, now: " + connections + " connections");
        } catch (IOException e) {
            System.err.println("Unable to accept channel");
            e.printStackTrace();
            sk.cancel();
        }
    }

    private void acceptConnections(int port) throws Exception {
        // Create a new server socket and set to non-blocking mode
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        // Bind the server socket to the port
        InetSocketAddress isa = new InetSocketAddress(port);
        ssc.socket().bind(isa);

        // Selector for incoming requests
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while ((selector.select()) > 0) {
            // Someone is ready for I/O, get the ready keys
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> i = readyKeys.iterator();

            // Walk through the ready keys collection and process requests.
            while (i.hasNext()) {
                SelectionKey sk = i.next();
                i.remove();
                if (sk.isAcceptable()) {
                    doAccept(sk);
                }
                else if (sk.isReadable()) {
                    System.out.println("Есть что почитать");
                    ((Translator) sk.attachment()).read();
                }
                else if (sk.isWritable()){
                    System.out.println("Есть шанс записать");
                    if(((Translator) sk.attachment()).write()) {
                        sk.interestOps(SelectionKey.OP_READ);
                    }
                    else {
                        System.out.println("Не получилось нормально записать :(");
                    }
                }
            }
        }
    }
}
