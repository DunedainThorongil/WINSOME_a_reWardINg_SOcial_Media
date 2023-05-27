import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Classe che si occupa di instaurare una connessione TCP sulla porta nota.
 * Quando viene accettata una connessione il threadpool va a gestire un task che si occupa dell'interazione tra un utente
 * e il sistema.
 */
public class GestoreTask {
    private final Integer TCP_PORT;
    private final ConcurrentHashMap <String, Utente> utentiRegistrati;
    private final ConcurrentHashMap<Integer, Post> posts;
    private final AtomicInteger idPost;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers;
    private final ConcurrentLinkedQueue<String> listaUtentiMulticast;
    private final ConcurrentLinkedQueue <String> listaUtentiOnline;
    private final RMICallbackServer notificationServer;
    private final String multicast;
    private final int MCASTPORT;
    private ThreadPoolExecutor threadPoolExecutor;


    public GestoreTask(ConcurrentHashMap<String, Utente> utentiRegistrati, ConcurrentHashMap<Integer, Post> posts, AtomicInteger idPost,
                       ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers, ConcurrentLinkedQueue<String> listaUtentiMulticast,
                       ConcurrentLinkedQueue <String> listaUtentiOnline,
                       Integer tcp_port, RMICallbackServer notificationServer, String multicast, int MCASTPORT){
        this.utentiRegistrati = utentiRegistrati;
        this.posts = posts;
        this.idPost = idPost;
        this.followers = followers;
        this.listaUtentiMulticast = listaUtentiMulticast;
        this.listaUtentiOnline = listaUtentiOnline;
        TCP_PORT = tcp_port;
        this.notificationServer = notificationServer;
        this.multicast = multicast;
        this.MCASTPORT = MCASTPORT;

    }

    public void start(){

        try (ServerSocket serverSocket = new ServerSocket()){
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), TCP_PORT ));
            System.out.println("\nComunicazione TCP stabilita!\nIn attesa di connessioni...");
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("\nUn nuovo client si è connesso");
                // per ogni client connesso si genera un ogetto Task che si occuparà di gestire le richieste da parte del client
                Task task = new Task(socket, utentiRegistrati, posts, idPost, followers, listaUtentiMulticast, listaUtentiOnline, notificationServer, multicast, MCASTPORT);

                try{
                    threadPoolExecutor.execute(task);
                }catch (java.util.concurrent.RejectedExecutionException | NullPointerException e){
                    e.printStackTrace();

                }

            }

        } catch (IOException  |SecurityException | IllegalArgumentException e) {
            threadPoolExecutor.shutdown();
            e.printStackTrace();
        }

    }

}
