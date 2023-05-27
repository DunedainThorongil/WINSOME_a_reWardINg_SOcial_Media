import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WinsomeServerMain {

    private static int TCP_PORT;
    private static String MULTICAST;
    private static int MCASTPORT;
    private static int RMI_PORT;
    private static int ATTESA;
    private static int RMI_CALLBACK_PORT;
    private static int PERCENTUALE_AUTORE;

    /* file usati per configurare e salvare */
    private static final String CLASS_PATH_FILE= "file";
    private static final String NOME_FILE = "/SpecificheServer.txt";
    private static final String NOME_FILE_JSON_UTENTI = "/RegistroUtenti.json";
    private static final String NOME_FILE_JSON_POST = "/RegistroPost.json";
    private static final String NOME_FILE_JSON_FOLLOWERS = "/RegistroFollowers.json";

    /*
     * Il server decide quale sarà il nome dei due servizi remoti che vuole mettere a disposizione. Il primo è quello
     * di registrazione alla piattaforma, il secondo è quello di iscrizione al sistema di notifiche.
     */
    private final static String registrationServiceName = "Register User";
    private final static String notycationServiceName = "Notifica";

    /*
     * istanza di RMICallbackserver per andare a definire l'oggetto remoto necessario per l'operazione
     * di callback
     */
    private static RMICallbackServer NotificationService;

    /**
     * Istaziamo una ConcurrentHashMap per gli utenti registrati al servizio Winsome, una per i posts contenuti all'interno
     * di Winsome e una per i followers
     */
    private static ConcurrentHashMap<String, Utente> utenti;
    private static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers;
    private static ConcurrentHashMap<Integer, Post> posts;
    private static AtomicInteger idP;
    private static ConcurrentLinkedQueue<String> listaUtentiMulticast;
    private static ConcurrentLinkedQueue <String> listaUtentiOnline;

    //variabili per generare l'oggetto remoto
    private static RMIRegistrazione registrazione;
    private static RMIRegistrazione_Interface stub;
    private static Registry registry;

    public static void main(String[] args) throws RemoteException {
        utenti = new ConcurrentHashMap<>();
        followers = new ConcurrentHashMap<>();
        posts = new ConcurrentHashMap<>();
        listaUtentiMulticast = new ConcurrentLinkedQueue<>();
        listaUtentiOnline = new ConcurrentLinkedQueue<>();

        NotificationService = new RMICallbackServer();
        idP = new AtomicInteger();

        System.out.println();
        System.out.println("Ripristino del sistema in corso...");
        System.out.println();

        ripristinoDati();
        System.out.println();

        if (!utenti.isEmpty()){
            for (String s: utenti.keySet()){
                System.out.println("Nome utente: " + s);
                System.out.println();
                System.out.println("\tDati utente: \n\t\tPassword: " + utenti.get(s).getPassword() + "\n\t\tTags: " + utenti.get(s).getTags());
                System.out.println();
                if (!utenti.get(s).getListaPosts().isEmpty()) {
                    System.out.println("\tPosts utente: ");
                    for (Post p: utenti.get(s).getListaPosts()) {
                        System.out.println("\n\t\tidPost: " + p.getIdPost() + "\n\t\tTitolo: " + p.getTitolo());
                    }
                } else {
                    System.out.println("\tPosts utente: \n\t\tNessun post ancora pubblicato!" );
                }
            }
        }

        if (!posts.isEmpty()){
            System.out.println("\nLista dei post nel sistema: ");
            for (Post p: posts.values()) {
                System.out.println("\n\t\tidPost: " + p.getIdPost() + "\n\t\tTitolo: " + p.getTitolo());
            }
        }

        if (!followers.isEmpty()){
            System.out.println(followers);
        }

        // si leggono le specifiche dal file di configurazione
        configurazione();

        //generazione del servizio remoto per la registrazione
        generazioneRegistrazione();

        //generazione del servizio remoto per la registrazione alle callback
        generazioneCallback();

        System.out.println();
        System.out.println("Porta TCP su cui verrà stabilita la connessione: " + TCP_PORT);
        System.out.println("Indirizzo multicast su cui verrà stabilita la connesio UDP: " + MULTICAST);

        //Thread che si occuperà di calcolare le ricompense di ogni utente
        Thread multicast = new Thread(new TaskServerMulticast(MULTICAST, MCASTPORT, utenti, ATTESA, PERCENTUALE_AUTORE));
        multicast.start();
        //threadMulticast.execute(new TaskServerMulticast(MULTICAST, MCASTPORT, utenti, ATTESA, PERCENTUALE_AUTORE));

        new GestoreTask(utenti, posts, idP, followers, listaUtentiMulticast,  listaUtentiOnline, TCP_PORT, NotificationService, MULTICAST, MCASTPORT).start();

    }

    /**
     * @effect: genera l'oggetto remoto messo a disposizione per il client per poter effetuare la registrazio al servizio Winsome
     */
    private static void generazioneRegistrazione(){

        try {
            registrazione = new RMIRegistrazione(utenti, followers);
            // creazione dello stub
            stub = (RMIRegistrazione_Interface) UnicastRemoteObject.exportObject(registrazione, 0);
            // creazione del registro
            LocateRegistry.createRegistry(RMI_PORT);
            // pubblicazione del registry e dello stub
            registry = LocateRegistry.getRegistry(RMI_PORT);
            registry.rebind(registrationServiceName, stub);

        } catch (RemoteException e){
            e.printStackTrace();

        }


    }

    /**
     * @effects: genera l'oggetto remoto necessario per consentire all'utente di iscriversi alle callback
     */
    private static void generazioneCallback() {

        try {
            NotificationService = new RMICallbackServer();
            RMICallbackServer_Interface stub = (RMICallbackServer_Interface) UnicastRemoteObject.exportObject(NotificationService, 0);
            Registry registry = LocateRegistry.createRegistry(RMI_CALLBACK_PORT);
            registry.rebind(notycationServiceName, stub);

        }catch (RemoteException e){
            e.printStackTrace();

        }

    }

    /* =================================================================================================================*/
    public static void configurazione(){

        File directory = new File(CLASS_PATH_FILE);
        if (!directory.isDirectory()){
            System.out.println(CLASS_PATH_FILE + " non è una directory valida!");
        }


        try (BufferedReader reader = new BufferedReader(new FileReader(CLASS_PATH_FILE + NOME_FILE)) ){

            String line = reader.readLine();

            while (line != null){
                if (!line.isBlank() && !line.startsWith("#")){

                    String[] dato = line.split("=");
                    String d = dato[0];

                    try {

                        switch (d) {
                            case "TCPPORT" -> {
                                String b = dato[1];
                                TCP_PORT = Integer.parseInt(b);
                                line = reader.readLine();
                            }
                            case "MULTICAST" -> {
                                MULTICAST = dato[1];
                                line = reader.readLine();
                            }
                            case "MCASTPORT" -> {
                                MCASTPORT = Integer.parseInt(dato[1]);
                                line = reader.readLine();
                            }
                            case "REGPORT" -> {
                                String e = dato[1];
                                RMI_PORT = Integer.parseInt(e);
                                line = reader.readLine();
                            }
                            case "ATTESA" -> {
                                ATTESA = Integer.parseInt(dato[1]);
                                line = reader.readLine();
                            }
                            case "RMICALLBACKPORT" -> {
                                String f = dato[1];
                                RMI_CALLBACK_PORT = Integer.parseInt(f);
                                line = reader.readLine();
                            }
                            case "PERCENTUALEAUTORE" -> {
                                PERCENTUALE_AUTORE = Integer.parseInt(dato[1]);
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

    /**
     * Salvataggio della struttura dati 'utenti' su file
     */
    public static void salvaUtente() {

        // Instazio GSON
        Gson gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .create();

        try (BufferedWriter writer = new BufferedWriter(new PrintWriter(CLASS_PATH_FILE + NOME_FILE_JSON_UTENTI))){
            Type utenteType = new TypeToken<ConcurrentHashMap<String, Utente>>(){}.getType();
            String s = gson.toJson(utenti, utenteType);

            writer.write(s);
            writer.flush();

            } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Uguale al metodo precendete, in questa si salva la struttura dati 'posts' su un file
     */
    public static void salvaPosts(){
        // Instazio GSON
        Gson gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .create();

        try (BufferedWriter writer = new BufferedWriter(new PrintWriter(CLASS_PATH_FILE + NOME_FILE_JSON_POST))){
            Type postType = new TypeToken<ConcurrentHashMap<Integer, Post>>(){}.getType();
            String s = gson.toJson(posts, postType);

            writer.write(s);
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Uguale al metodo precendete, in questa si salva la struttura dati 'followers' su un file
     */
    public static void salvaFollowers(){
        // Instazio GSON
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try (BufferedWriter writer = new BufferedWriter(new PrintWriter(CLASS_PATH_FILE + NOME_FILE_JSON_FOLLOWERS))){
            Type followType = new TypeToken<ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>(){}.getType();
            String s = gson.toJson(followers, followType);

            writer.write(s);
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    Metodi per recuperare i dati dai file
     */
    public static void ripristinaUtene(){
        Gson gson = new Gson();
        try (BufferedReader reader = new BufferedReader(new FileReader(CLASS_PATH_FILE + NOME_FILE_JSON_UTENTI))){
            Type utenteType = new TypeToken<ConcurrentHashMap<String, Utente>>(){}.getType();
            ConcurrentHashMap<String, Utente> u = gson.fromJson(reader, utenteType);

            if (u != null){
                utenti.putAll(u);
                System.out.println("Backup degli Utenti effettuato!");
            } else {
                System.out.println("Sistema vuoto!");
            }

        } catch (FileNotFoundException f){

            try{
                System.out.println("Sistema vuoto!");
                File fi = new File(CLASS_PATH_FILE + NOME_FILE_JSON_UTENTI);
                if(fi.createNewFile()){
                    System.out.println("File 'RegistroUtenti.json' creato!");
                }else {
                    System.out.println("Errore nella creazione del file di nome 'RegistroUtenti.json'!");
                }

            } catch (IOException e){
                e.printStackTrace();
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void ripristinaPost(){
        Gson gson = new Gson();

        try (BufferedReader reader = new BufferedReader((new FileReader(CLASS_PATH_FILE + NOME_FILE_JSON_POST)))){
            Type postType = new TypeToken<ConcurrentHashMap<Integer, Post>>(){}.getType();
            ConcurrentHashMap<Integer, Post> p = gson.fromJson(reader, postType);

            if (p != null){
                posts.putAll(p);
                for (Post pos: posts.values()) {
                    if(pos.getIdPost() > idP.get()){
                        idP = new AtomicInteger(pos.getIdPost());
                    }
                }
                System.out.println("Backup dei Post effettuato!");
            }

        }  catch (FileNotFoundException f){

            try{
                File fi = new File(CLASS_PATH_FILE + NOME_FILE_JSON_POST);

                if (fi.createNewFile()){
                    System.out.println("File 'RegistroPost.json' creato!");
                }else {
                    System.out.println("Errore nella creazione del file 'RegistroPost.json.json' !");
                }
            } catch (IOException e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void ripristinaFollowers(){
        Gson gson = new Gson();

        try (BufferedReader reader = new BufferedReader((new FileReader(CLASS_PATH_FILE + NOME_FILE_JSON_FOLLOWERS)))){
            Type followType = new TypeToken<ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>>(){}.getType();
            ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> f = gson.fromJson(reader, followType);

            if (f != null){
                followers.putAll(f);
                System.out.println("Backup dei Followers effettuato!");
            }

        }  catch (FileNotFoundException f){

            try{
                File fi = new File(CLASS_PATH_FILE + NOME_FILE_JSON_FOLLOWERS);
                if (fi.createNewFile()){
                    System.out.println("File 'RegistroFollowers.json' creato!");
                } else {
                    System.out.println("Errore nella creazione del file 'RegistroFollowers.json' !");
                }

            } catch (IOException e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void ripristinoValori(){
        if (posts != null) {
            for (Post p: posts.values()) {
                if (utenti.containsKey(p.getAutore().getNome())){
                    Utente u = utenti.get(p.getAutore().getNome());
                    u.setListaPosts(p);
                }
            }
        }

    }

    public static void ripristinoDati(){
        ripristinaUtene();
        ripristinaPost();
        ripristinaFollowers();
        //utilizzo i dati appena raccolti dai metodi 'rispristinaPost' e 'ripristinaFollowers' per completare i dati degli utenti
        ripristinoValori();
    }

}
