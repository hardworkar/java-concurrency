public class Main {
    public static void main(String[] args) throws Exception {
        int portNumberListenTo = Integer.parseInt(args[0]);
        String hostTranslateTo = args[1];
        int portNumberTranslateTo = Integer.parseInt(args[2]);

        Server server = new Server(portNumberListenTo, hostTranslateTo, portNumberTranslateTo);
    }
}
