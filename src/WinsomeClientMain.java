import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class WinsomeClientMain {

    private static int TCP_PORT;
    private static int RMI_PORT;
    private static int RMI_CALLBACK_PORT;
    private static String MULTICAST;
    private static int PORTA_MULTICAST;

    private static final String CLASS_PATH_FILE= "file";
    private static final String NOME_FILE = "/SpecificheClient.txt";

    public static String messaggio;
    public static boolean isOnline;
    public static String username; //nome dell'utente che effetua il login

    /*
     * Il client deve conoscere il nome dei servizi remoti per poterli reperire e utilizzare
     */
    private final static String registrationServiceName = "Register User";
    private final static String notifyServiceName = "Notifica";

    private static RMICallbackServer_Interface callbackServer;
    private static RMICallbackClient_Interface callbackObject;
    private static RMICallbackClient_Interface stub;

    /*
     * Si definiscono due buffer per lo stream di byte tra il client al server.
     */
    private static BufferedReader reader;
    private static BufferedWriter writer;

    // struttura dati che viene aggiornata con le callback
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers = new ConcurrentHashMap<>();
    // lista degli utenti online, che viene usata per garantire che uno stesso utente non si possa loggare da due client diversi
    private static final ConcurrentLinkedQueue<String> utentiOnline = new ConcurrentLinkedQueue<>();
    // thread che, nel caso in cui viene eseguita l'iscrizione al multicast, rimane in attesa per ricevere le notifiche dal multicast
    private static Thread t;


    public static void main(String[] args) {
        isOnline = false;
        /*
         * Il client acquisisce le infomazioni di configurazione dal file 'SpecificheClient.txt'
         */
        configurazione();

        try (Socket socket = new Socket()) {
            /*
             * Si effettua la connessione tramite TCP e si preparano i buffer di scrittura e lettura
             */
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), TCP_PORT));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            /*
             * Si preparano le informazioni dell'oggetto remoto per l'iscrizione al servizio di Callback. Il reperimento
             * dello stub necessario per la registrazione alla piattaforma è intrinseco all'operazione di registrazioneCallback
             */
            notificaClient();

            System.out.println("< Benvenuta/o su Windsome!");
            System.out.println("\tWinsome è una piattaforma social basata su blockchain in cui vengono premiate le idee!");
            System.out.println();
            System.out.println("\tPer registrarti digita: register <username> <password> <tag_1,...,tag_N>" +
                    "\n\tdove al posto di 'tag_1,..,tag_N' inserirai la lista dei tuoi interessi");
            System.out.println("\tSei già registrato? Per effetuare il login digita: login <username> <password>");
            System.out.println();
            System.out.println("\tHai bisogno di aiuto? Digita help");
            System.out.println();
            System.out.println("\tPer chiudere la connesione digita logout <username>");
            System.out.print("> ");

            /*
             * Invoco la funzione che si occuopa di parsare i comandi da linea di comando ed di invairli al Server e valutarli
             */
            letturaCLI();


        } catch (ConnectException e) {
            System.out.println("Connesione rifiutata");
        } catch (IOException c) {
            c.printStackTrace();
        }

    }

    /* ============================================================================================================== */

    public static void configurazione(){

        File directory = new File(CLASS_PATH_FILE);
        if (!directory.isDirectory()){
            System.out.println(CLASS_PATH_FILE + " non è una directory valida!");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CLASS_PATH_FILE + NOME_FILE))){

            String line = reader.readLine();

            while (line != null){
                if (!line.isBlank() && !line.startsWith("#")){ // vengono ignorate tutte le linee che sono vuote o iniziano con #

                    String[] dato = line.split("=");
                    String d = dato[0];

                    try {
                        switch (d) {
                            case "TCPPORT" -> {
                                String b = dato[1];
                                TCP_PORT = Integer.parseInt(b);
                                line = reader.readLine();
                            }
                            case "REGPORT" -> {
                                String e = dato[1];
                                RMI_PORT = Integer.parseInt(e);
                                line = reader.readLine();
                            }
                            case "RMICALLBACKPORT" -> {
                                String f = dato[1];
                                RMI_CALLBACK_PORT = Integer.parseInt(f);
                                line = reader.readLine();
                            }
                            default -> line = reader.readLine();
                        }

                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }else {
                    line = reader.readLine();
                }

            }

        } catch (IOException e){
            e.printStackTrace();

        }

    }

    public static void letturaCLI() {
        try (BufferedReader cmd = new BufferedReader(new InputStreamReader(System.in))) {
            messaggio = cmd.readLine();

            while (!messaggio.equals("close")) {
                String[] input = messaggio.split(" ");
                String operazione = input[0];
                /*
                 * così come per il server, si usa uno switch per gestire le possibili stringhe ricevuta dalla CLI
                 */
                try {
                    String risposta;
                    switch (operazione) {

                        case "close" :
                            if (input.length == 1){
                                isOnline = false;
                                return;
                            }
                            break;

                        case "logout":
                            if (input.length == 2){
                                sendRequest();
                                while (!(risposta = reader.readLine()).equals("")) {
                                    if (!risposta.startsWith("Errore")){
                                        isOnline = false;
                                        try {
                                            callbackServer.rimuoviRegistrazioneCallback(username, stub);
                                            utentiOnline.remove(username);

                                        } catch (RemoteException e){
                                            e.printStackTrace();
                                        }
                                    }
                                    System.out.println("< " + risposta);
                                }
                            } else {
                                risposta = "Errore, digita logout <username>";
                                System.out.println("< " + risposta);
                            }
                            break;

                        case "partecipa":
                            sendRequest();
                            while (!(risposta = reader.readLine()).equals("")){
                               if (!risposta.startsWith("Errore")) {
                                   String[] r = risposta.split(" ");
                                   MULTICAST = r[0];
                                   PORTA_MULTICAST = Integer.parseInt(r[1]);
                                   t = new Thread(new TaskClientMulticast(PORTA_MULTICAST, MULTICAST));
                                   t.start();
                               }
                               System.out.println("< " + risposta);
                            }
                            break;

                        case "login":
                                try{
                                    callbackServer.registrazioneCallback(input[1], stub);
                                } catch (RemoteException e){
                                    System.out.println("[ Errore registrazione alle notifiche! ]");
                                }

                            sendRequest();
                            while (!(risposta = reader.readLine()).equals("")) {
                                if (!risposta.startsWith("Errore")){
                                    isOnline = true;
                                    username = input[1];
                                    utentiOnline.add(username);
                                } else {
                                    try {
                                        callbackServer.rimuoviRegistrazioneCallback(input[1], stub);
                                    } catch (RemoteException e){
                                        System.out.println("[ Errore rimozione dalle notifiche, da login fallito! ]");
                                    }
                                }
                                System.out.println("< " + risposta);
                            }
                            break;

                        case "register":
                            if (!isOnline){
                                if (input.length >= 4) {
                                    String username = input[1];
                                    String password = input[2];
                                    // tags massimo 5
                                    Vector<String> tags = new Vector<>(Arrays.asList(input).subList(3, input.length));
                                    //chiamata a metodo remoto
                                    register(username, password, tags);

                                } else {
                                    risposta = "Errore, inseriti numero di parametri errati!\nDigitare register <username> <password> <tags>\nI tags possono essere al massimo 5";
                                    System.out.println("< " + risposta);
                                }
                                break;

                            }else {
                             risposta = "Errore, " + username + " online, non puoi registrarti!";
                                System.out.println("< " + risposta);
                            }
                            break;

                        case "list":
                            if (isOnline) {
                                if (input.length == 2) {
                                    String opCompleta = input[1];
                                    if (opCompleta.equals("followers")) {
                                        risposta = listFollowers();
                                        System.out.println("< " + risposta);
                                    } else {
                                        // i casi di 'list users' e 'list following' vengono gestiti dal Server
                                        sendRequest();
                                        while (!(risposta = reader.readLine()).equals("")) {
                                            System.out.println("< " + risposta);
                                        }
                                        break;
                                    }

                                } else {
                                    risposta = "Errore: numero parametri errati";
                                    System.out.println("< " + risposta);
                                }
                            } else{
                                risposta = "Errore, utente non online!";
                                System.out.println("< " + risposta);
                            }
                            break;

                        case "help":
                            mostraListaOperazioni();
                            break;

                        default:
                            // tutte le altre operazioni vengono gestite lato Server
                            sendRequest();
                            try {
                                //System.out.println(reader.readLine()); //debug
                                while (!(risposta = reader.readLine()).equals("")) {
                                    System.out.println("< " + risposta);
                                }

                            } catch (NullPointerException e){ // viene gestito il caso in cui il server viene interrotto e non manda più risposte al client
                                System.out.println("Nessuna risposta ricevuta del Server, disconnessione in corso!");

                                isOnline = false;
                                System.out.println(username + " disconnesso!");
                                utentiOnline.remove(username);

                                try {
                                    callbackServer.rimuoviRegistrazioneCallback(username, stub);
                                    utentiOnline.remove(username);

                                } catch (RemoteException ex){
                                    ex.printStackTrace();
                                }

                                return;
                            }
                            break;
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

                System.out.println();
                System.out.print("> ");
                messaggio = cmd.readLine();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @effects: metodo di supporto che si occupa di inviare la richiesta al server. Viene utilizzano per evitare la
     * ridondanza di codice nel parser dei comandi
     */
    public static void sendRequest() {

        try {
            writer.write(messaggio + "\r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Operazione lato client per visualizzare la lista dei propri follower.
     * Questo comando dell’utente non scatena una richiesta sincrona dal client al server.
     * Il client restituisce la lista dei follower mantenuta localmente che viene via via aggiornata grazie a notifiche
     * “asincrone” ricevute dal server. Vedere i dettagli di implementazione nella sezione successiva.
     *
     * @return : la lista dei followers
     */
    public static String listFollowers() {
        String risp = "Lista dei tuoi followers: ";

        if (followers.containsKey(username)){
           risp += followers.get(username);
        }

        return risp;

    }

    /**
     * @effect: stampa la lista di tutte le operazioni che si possono usare su Winsome
     */
    public static void mostraListaOperazioni() {

        List<String> listaMetodi = Arrays.asList(
                "\tregistrazione --> register <username> <password> <tag_1,...,tag_N>",
                "\tlogin --> login <username> <password>",
                "\tlogout--> logout <username>",
                "\tlista degli utenti con tags in comune --> list users",
                "\tlista dei propri followers --> list followers <username>",
                "\tseguire un utente --> follow <username>",
                "\tsmettere di seguire un utente --> unfollow <username>",
                "\tlista dei posti pubblicati --> blog",
                "\tcrea un post --> post <title> <content>",
                "\tvedi i posts del tuo feed --> show feed",
                "\tmosta un post --> show post <id>",
                "\tcancella un post --> delete <idPost>",
                "\trewin un post --> rewin <idPost>",
                "\tassegna un voto --> rate <idPost> <vote>",
                "\taggiungi un commento --> comment <idPost> <comment>",
                "\tvisualizza il portafoglio --> wallet",
                "\trepurare il valore del proprio portafoglio convertito in BITCOIN --> wallet btc",
                "\tpartecipare al gruppo multicast --> partecipa",
                "\ttermina connessione --> control + c");

        for (String s : listaMetodi) {
            System.out.println(s);
        }

    }

    /**
     * Per inserire un nuovo utente, il server mette a disposizione una operazione di registrazione di un utente.
     * L’utente deve fornire username, password e una lista di tag (massimo 5 tag). Il server risponde con un codice
     * che può indicare l’avvenuta registrazione, oppure, se lo username è già presente, o se la password è vuota, o
     * la lista dei tags è troppo lunga (sono ammessi al massimo 5 tags) restituisce un messaggio d’errore.
     * L'username dell’utente deve essere univoco.
     *
     * @param username nome dell'utente
     * @param password password personale dell'utente
     * @param tags lista di tags dell'utente
     */
    private static void register(String username, String password, Vector<String> tags) {
        String risp;

        try {
            // usato il procedimento visto a lezione
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);

            RMIRegistrazione_Interface reg = (RMIRegistrazione_Interface) registry.lookup(registrationServiceName);
            risp = reg.registrazione(username, password, tags);
            System.out.println("> " + risp);

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.err.println("Errore nel metodo di registrazione lato RMI Client");

        }

    }

    private static void notificaClient() {
        try {
            // usato il procedimento visto a lezione
            Registry registry = LocateRegistry.getRegistry(RMI_CALLBACK_PORT);
            callbackServer = (RMICallbackServer_Interface) registry.lookup(notifyServiceName);

            callbackObject = new RMICallbackClient(followers, utentiOnline);
            stub = (RMICallbackClient_Interface) UnicastRemoteObject.exportObject(callbackObject, 0);

        } catch (NotBoundException | RemoteException e) {
            e.printStackTrace();
        }

    }

}
