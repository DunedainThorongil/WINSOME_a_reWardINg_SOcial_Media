/*
 * Classe che modella l'handler di gestione delle richieste: quando il client fa richiesta di una determinata
 * operazione quest'ultima viene passata all'handler che userà il metodo più opportuno per restituire la risposta
 * al client.
 * Tutti gli handler di richiesta sono gestiti da un threadPool.
 */

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Task implements Runnable{

    private Utente utente;
    private boolean isOnline;
    private final ConcurrentHashMap<String, Utente> utenti;
    private final ConcurrentHashMap<Integer , Post> posts;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers;

    private final ConcurrentLinkedQueue<String> listaUtentiMulticast;
    private final ConcurrentLinkedQueue <String> listaUtentiOnline;
    private final Socket clientSocket;
    private final AtomicInteger idP;

    // variabili usate per creare post e commenti
    private String titolo;
    private String contenuto;
    private String commento;

    /**
     * Riferimento all'oggeto remoto
     */
    private final RMICallbackServer notificationService;

    //indirizzo multicast
    private final String multicast;
    private final int MCASTPORT;

    //stringa di risposta
    public String risposta;

    //Array contenente 15 tassi di cambio generati da RANDOM.ORG
    public final Vector<Double> arrayTassoDiCambio ;

    public Task(Socket clientSocket, ConcurrentHashMap<String , Utente> utenti, ConcurrentHashMap<Integer , Post> posts,
                AtomicInteger idP, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers, ConcurrentLinkedQueue<String> listaUtentiMulticast,
                ConcurrentLinkedQueue <String> listaUtentiOnline, RMICallbackServer rmiCallbackServerServer,
                String multicast, int MCASTPORT){

        this.clientSocket = clientSocket;
        this.utenti = utenti;
        this.posts = posts;
        this.idP = idP;
        this.followers = followers;
        this.listaUtentiMulticast = listaUtentiMulticast;
        this.listaUtentiOnline = listaUtentiOnline;

        this.notificationService = rmiCallbackServerServer;
        this.multicast = multicast;
        this.MCASTPORT = MCASTPORT;

        arrayTassoDiCambio = new Vector<>();
        isOnline = false;

    }

    @Override
    public void run() {
        String richiesta;

        while (true){

            //creo il canale di comunicazione tra Server e Client
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

                while ((richiesta = reader.readLine()) != null){

                    System.out.println("< Client: " + richiesta);
                    String[] input = richiesta.split(" ");
                    String[] inputCreaPostCommento = richiesta.split("\"");
                    String operazione = input[0];
                    /*
                     * Viene fatto un controllo con la prima parola riscontrata: in base al comando letto vien eseguita
                     * una specifica funzione dopo aver fatto un controllo sugli argomenti passati.
                     * Se non viene passato il controllo sugli argomenti si restituisce direttamente l'errore al client
                     */
                    switch (operazione){

                        case "logout":
                            if (isOnline){
                                if (controlloParametri(input)){
                                    risposta = logout(input[1]);
                                    if (!risposta.startsWith("Errore")){
                                        listaUtentiOnline.remove(input[1]);
                                        listaUtentiMulticast.remove(input[1]);

                                        WinsomeServerMain.salvaUtente();
                                        WinsomeServerMain.salvaPosts();
                                        WinsomeServerMain.salvaFollowers();
                                    }
                                    break;
                                }
                            } else {
                                risposta = "Errore, non sei online!";
                            }
                            break;

                        case "partecipa":
                            if (isOnline){
                                if (input.length == 1){
                                    risposta = multicast + " " + MCASTPORT + " indirizzo e porta multicast a cui verrai iscritto!";
                                } else {
                                    risposta = "Errore, digita soltanto 'partecipa'";
                                }
                            } else {
                                risposta = "Errore, non sei online!";
                            }
                            break;

                        case "login" :
                            // un utente può essere loggato in un solo client per volta
                            try{
                                if (utenti.get(input[1]) != null && !isOnline && !listaUtentiOnline.contains(input[1])){
                                    if (controlloParametri(input)) {
                                        String nome = input[1];
                                        String password = input[2];
                                        risposta = login(nome, password);
                                        if (!risposta.startsWith("Errore")){
                                            listaUtentiOnline.add(nome);
                                        }
                                        break;
                                    }
                                }
                                if (utenti.get(input[1]) == null){
                                    risposta = "Errore, nessun utente è registrato con questo nome!";
                                    break;
                                }
                                if (isOnline) {
                                    risposta = "Errore: " + utente.getNome() + " già loggato!";
                                    break;
                                }
                                if (listaUtentiOnline.contains(input[1])){
                                    risposta = "Errore, " + input[1] + " già loggato da un altro terminale!";
                                    break;
                                }
                                break;
                            }catch (NullPointerException e){
                                risposta = "Errore, interire valore valido";
                                break;
                            }

                        case "list" : // nel caso 'list' vengono analizzati sia list users che list following
                            if (isOnline){
                                if (controlloParametri(input)){
                                    if (input[1].equals("users")){
                                        risposta = listUsers();
                                        break;
                                    }
                                    if (input[1].equals("following")){
                                        risposta = listFollowing();
                                    }else {
                                        risposta = "Errore! Digitare 'list users/following'!";
                                    }
                                    break;
                                }

                            }else {
                                risposta = "Errore, utente non online!";
                                break;
                            }
                            break;

                        case "follow" :
                            if (isOnline){
                                if (controlloParametri(input)){
                                    String idUser = input[1];
                                    risposta = followUser(idUser);
                                    break;
                                }
                            }else {
                                risposta = "Errore, utente non online!";
                                break;
                            }
                            break;

                        case "unfollow" :
                            if (isOnline){
                                if (controlloParametri(input)){
                                    String idUser = input[1];
                                    risposta = unfollowUser(idUser);
                                    break;
                                }
                            } else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "blog" :
                            if (isOnline){
                                if (controlloParametri(input)){
                                    risposta = viewBlog();
                                    break;
                                }
                            } else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "post" :
                            if (isOnline){
                                if (controlloParametri(input)) {
                                    if (controlloParametriPost(inputCreaPostCommento)){
                                        risposta = createPost(titolo, contenuto);
                                        break;
                                    }
                                }
                            }else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "show" : // show feed e show post vengono analizzati nello stesso case
                            if (isOnline){
                                if (controlloParametri(input)){
                                    if (input[1].equals("feed")){
                                        risposta = showFeed();
                                        break;
                                    }
                                    if(input[1].equals("post")){
                                        int idPost = Integer.parseInt(input[2]);
                                        risposta = showPost(idPost);
                                        break;

                                    } else {
                                        risposta = "Errore! Digitare 'show feed/post'!";
                                    }
                                    break;
                                } else {
                                    risposta = "Errore! Digitare 'show feed/post'!";
                                }
                            } else {

                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "delete" :
                            if (isOnline){
                                if(controlloParametri(input)){
                                    int idPost = Integer.parseInt(input[1]);
                                    risposta = deletePost(idPost);
                                    break;
                                }
                            } else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "rewin" :
                            if (isOnline){
                                if (controlloParametri(input)){
                                    int idPost = Integer.parseInt(input[1]);
                                    risposta = rewinPost(idPost);
                                }
                            } else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "rate" :
                            if (isOnline){
                                if (controlloParametri(input)){
                                    int idPost = Integer.parseInt(input[1]);
                                    int voto = Integer.parseInt(input[2]);
                                    risposta = ratePost(idPost, voto);
                                    break;
                                }
                            } else {
                               risposta = "Errore, utente non online!";
                            }
                            break;

                        case "comment" :
                            if (isOnline){
                                if (controlloParametri(input)){
                                    if (controlloParametriCommento(inputCreaPostCommento)){
                                        int idPost = Integer.parseInt(input[1]);
                                        risposta = addComment(idPost, commento);
                                        break;
                                    }
                                }
                            } else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        case "wallet" :  //vengono gestiti sia i casi di 'wallet' che di di 'wallet btc'
                            if (isOnline){
                                if (input.length == 1){
                                    risposta = getWallet();
                                    break;
                                } else if (input.length == 2){
                                    if (input[1].equals("btc")){
                                        risposta = getWalletInBitcoin();
                                        break;
                                    }
                                } else {
                                    risposta = "Errore, digitare 'wallet' oppure 'wallet btc'!";
                                    break;
                                }
                                break;
                            }else {
                                risposta = "Errore, utente non online!";
                            }
                            break;

                        default:
                            risposta = "Operazione non valida, per vedere tutte le operazioni valide digita 'help'!";
                            break;
                    }
                    /*
                     * La risposta ad una richiesta eseguta dal server viene restituita all'utente
                     */
                    writer.write(risposta + "\n\r\n");
                    writer.flush();
                }
            } catch (NullPointerException | InterruptedException | ExecutionException n){
                n.printStackTrace();

            } catch (IOException e){

                System.out.println("> un client si è scollegato dal sistema ");
                // anche in caso di uscita forzata, con il comando control + c,
                // vengono salvati i dati in maniera persistente sui file
                WinsomeServerMain.salvaUtente();
                WinsomeServerMain.salvaPosts();
                WinsomeServerMain.salvaFollowers();

                break;
            }
        }

    }

    /**
     * Metodo usato per controllare se il numero di parametri passati da CLI è corretto
     *
     * @param input valore preso da CLI
     * @return se il controllo sul numero di elmeneti passati è corretto ritorna true, altrimentri false
     */
    private boolean controlloParametri(String[] input){
        String operazione = input[0];

        switch (operazione){

            case "login" :
                if (input.length != 3){
                    risposta = "Errore! Digitare 'login <username> <password>'!";
                    return false;
                }else {
                    return  true;
                }

            case "logout" :
                if (input.length != 2){
                    risposta = "Errore! Digitare 'logout <username>'!";
                    return false;
                } else {
                    return true;
                }

            case "list" : // nel caso list vengono analizzati sia list users/following
                if (input.length != 2){
                    risposta = "Errore! Digitare 'list users/following'!";
                    return false;
                } else {
                    return true;
                }

            case "follow" :
                if (input.length != 2){
                    risposta = "Errore! Digita 'follow <username>'!";
                    return false;
                } else {
                    return true;
                }

            case "unfollow" :
                if (input.length != 2){
                    risposta = "Errore! Digita 'unfollow <username>'!";
                    return false;
                } else {
                    return true;
                }

            case "blog" :
                if (input.length != 1){
                    risposta = "Errore! Digita 'blog'!";
                    return false;
                } else {
                    return true;
                }

            case "post" :
                if (input.length > 520){
                    risposta = "Errore! Digita 'post <title> <content>'!";
                    return false;
                } else {
                    return true;
                }

            case "show":
                String operazioneCompleta = input[1];

                if (operazioneCompleta.equals("post")){
                    if (input.length != 3){
                        risposta = "Errore! Digita 'show post <id>'!";
                        return false;
                    } else {
                        try {
                            Integer.parseInt(input[2]);
                        }catch (NumberFormatException e){
                            risposta = "Errore! L'idPost deve essere un numero intero!";
                            return false;
                        }
                        return true;
                    }
                }
                if (operazioneCompleta.equals("feed")){
                    if (input.length != 2){
                        risposta = "Errore! Digita 'show feed'!";
                        return false;
                    } else {
                        return true;
                    }
                }
                return false;

            case "delete" :
                if (input.length != 2){
                    risposta = "Errore! Digita 'delete <idPost>'!";
                    return false;
                } else {
                    try {
                        Integer.parseInt(input[1]);
                    }catch (NumberFormatException e){
                        risposta = "Errore! L'idPost deve essere un numero intero!";
                        return false;
                    }
                    return true;
                }

            case "rewin" :
                if (input.length != 2){
                    risposta = "Errore! Digita 'rewin <idPost>'!";
                    return false;
                } else {
                    try {
                        Integer.parseInt(input[1]);

                    }catch (NumberFormatException e){
                        risposta = "Errore! L'idPost deve essere un numero intero!";
                        return false;
                    }
                    return true;
                }

            case "rate" :
                if (input.length != 3){
                    risposta = "Errore! Digita 'rate <idPost> <vote>'!";
                    return false;
                } else {
                    try {
                        Integer.parseInt(input[1]);
                        Integer.parseInt(input[2]);
                    }catch (NumberFormatException e){
                        risposta = "Errore! Sia l'idPost che il voto devono essere numeri interi!";
                        return false;
                    }
                    return true;
                }

            case "comment" :
                if (input.length > 200){
                    risposta = "Errore. Digitare 'comment <idPost> <comment>'!";
                    return false;
                } else {
                    try {
                        Integer.parseInt(input[1]);
                    }catch (NumberFormatException e){
                        risposta = "Errore! L'idPost deve essere un numero intero!";
                        return false;
                    }
                    return true;
                }
        }
        return false;

    }

    /**
     * Metodo usato per controllare se la stringa del Titolo è lunga max 20 caratteri, e se i caratteri del contenuto
     * sono max 500
     * @param input preso dalla CLI
     * @return true se i parametri sono tutti corretti false altrimenti
     */
    private boolean controlloParametriPost(String[] input) {
        if (input.length == 4){
            //input[0] è la stringa post
            titolo = input[1];
            //input[2] è lo spazio
            contenuto = input[3];
            if (titolo.isBlank() || contenuto.isBlank()){
                risposta = "Errore, il titolo e/o il contenuto non posso essere formati dallo spazio vuoto o da una sua sequenza!";
                return false;
            } else if (titolo.length() > 20){
                risposta = "Errore, il titolo è troppo lungo, può essere al max di 20 caratteri!";
                return false;

            } else if (contenuto.length() > 500){
                risposta = "Errore, il contenuto è troppo lungo, può essere al massimo di 500 caratteri!";
                return false;

            } else{
                return true;
            }

        } else {
            risposta = "Errore, inserire sia titolo che commento tra \" \" !";
            return false;
        }

    }

    /**
     * Metodo usato per controllare se l'id del post è un numero e la stringa del commento non supera i 200 caratteri
     * @param input stinga presa da CLI e splittata al carattere '"'
     * @return true se i parametr sono corretti, false altrimenti
     */
    private boolean controlloParametriCommento(String[] input){
        if (input.length == 2){
            // input[0] è la stringa id
            commento = input[1];
            // input[2] spazio

            if (commento.length() > 200){
                risposta = "Errore, commento troppo lungo, non può superare i 200 caratteri!";
                return false;
            }
            if(commento.isBlank()){
                risposta = "Errore, il commento non può essere uno spazio o una sequenza di spazi";
                return false;
            }
        } else {
            risposta = "Errore, inserire il commento tra \" \" !";
            return false;
        }
        return true;

    }

    /**
     * Logout dell'utente
     * @param username nome dell'utente
     * @return stringa di risposta
     */
    private String logout(String username){
        String risp;

            try{
                if (utenti.get(username) == null){
                    risp = "Errore, " + username + " non è un utente di Winsome!";
                    return risp;
                }if (utenti.get(username) != this.utente){
                    risp = "Errore, non puoi scollegare un altro utente!";
                    return risp;
                }
                utente.setStato(false);
                isOnline = false;
                risp = this.utente.getNome() + " ti sei disconnesso!\nTorna presto su Winsome per guadagnare con le idee!";
                return risp;
            } catch (NullPointerException e){
                risp = "Il nome dell'utente non può essere null!";
                return risp;
            }

    }

    /**
     * Login di un utente già registrato per accedere al servizio. Il server risponde con un codice che può indicare
     * l’avvenuto login, oppure, se l’utente ha già effettuato la login o la password è errata, restituisce un messaggio
     * d’errore.
     * @param username nome dell'utente
     * @param password password associata all'utente
     * @return la stringa con la risposta del server da mandare al Client
     */
    public String login(String username, String password) {
        String risp;

        try{
            if (utenti.get(username) == null) {
                risp = "Errore, " + username + " non è un utente di Winsome!";
                return risp;
            } else if (!((utenti.get(username)).getPassword().equals(password))) {
                risp = "Errore, password sbagliata, per favore prova ancora!";
                return risp;

            }else {
                try {
                    // al login dell'utente gli viene comunicata la struttura dati aggiornata dei followers
                    notificationService.updateFollowers(username, this.followers);
                } catch (RemoteException e){
                    System.out.println("[ ERRORE: Callback updateFollowers ]");
                }

                risp = username + " sei online!";
                this.utente = utenti.get(username);
                this.utente.setStato(true); // ora l'utente è online
                isOnline = true;
            }

            return risp;
        }catch (NullPointerException e){
            risp = "Il nome utente non può essere null!";
            return risp;
        }

    }

    /**
     * Utilizzata da un utente per visualizzare la lista (parziale) degli utenti registrati al servizio.
     *
     * @return Il server restituisce la lista di utenti che hanno almeno un tag in comune con l’utente che ha
     * fatto la richiesta.
     */
    private String listUsers() {
        String risp = "Lista degli utenti con interessi in comune: ";
        Vector<String> utenteTags = this.utente.getTags();
        List<Utente> utentiTagsComuni = new LinkedList<>(); //inizialmente vuota

        // scorro tutti gli utenti iscritti a Winsome, e per ogni utente con almeno un tag in comune con 'this.utente'
        // lo aggiungo alla lista 'utentiTagsComuni'
        for (ConcurrentMap.Entry<String, Utente> map : utenti.entrySet()){ // weakly consistent
            Utente u = map.getValue();
            Vector<String> uTags = u.getTags();
            for (String s1 : utenteTags){
                if (uTags.contains(s1)){
                    if (!utentiTagsComuni.contains(u) && !u.getNome().equals(this.utente.getNome())){
                        utentiTagsComuni.add(u);
                        break;
                    }
                }
            }
        }

        if (utentiTagsComuni.size() == 0){
            risp += "Non ci sono utenti registrati con i tuoi stessi interessi!";

        } else {
            //inserisco la lista nella stringa di risposta
            for (Utente u: utentiTagsComuni) {
                risp += "\t" + u.getNome();
            }
        }

        return risp;

    }

    /**
     * Utilizzata da un utente per visualizzare la lista degli utenti di cui è follower.
     * @return una stringa contenetente lista dei followers
     */
    private String listFollowing() {
        Utente utente = this.utente;
        String risp = "Lista degli utenti che segui: ";
        List<String> listFollowing = new LinkedList<>();
        List<String> l = new CopyOnWriteArrayList<>(followers.keySet());

        //copio la struttura dati followers.get in un vector, la scorro e controllo usando 'followers' quali utenti 'this.utente' segue
        for (String u: l) {
            Vector<String> listF = new Vector<>(followers.get(u));
            for (String nome: listF) {
                if (utente.getNome().equals(nome)){
                   listFollowing.add(u);
                }
            }
        }
        risp += listFollowing.toString();
        return risp;

    }

    /**
     * L’utente chiede di seguire l’utente che ha per username idUser. Da quel momento in poi può 'ricevere tutti i post
     * pubblicati da idUser.
     * @param idUser nome dell'utente da seguire
     * @return una stringa contenetente il messaggio di risposta
     */
    private String followUser(String idUser) {
        String risp;

        try{
            if (utenti.get(idUser) == null) {
                risp = "Errore, " + idUser + " non è un utente di Winsome, non puoi seguirlo!";
                return risp;
            } else if (this.utente.getNome().equals(idUser)){
                risp = "Errore, non puoi seguirti da solo!";
                return risp;
            }else {
                Utente utenteDaSeguire = utenti.get(idUser);

                if (!followers.get(utenteDaSeguire.getNome()).contains(this.utente.getNome())){
                    followers.get(utenteDaSeguire.getNome()).add(this.utente.getNome());

                    risp = "Hai iniziato a seguire " + utenteDaSeguire.getNome() + "!\nDa ora potrai vedere ed interagire con tutti i suoi posts!";
                }else {
                    risp = "Errore, segui già " + idUser + "!";
                    return risp;
                }

                /*
                 * L'utente inizia a seguire l'altro utente, si notifica al client(utente seguito), se online,  tramite callbacks.
                 * Nel caso in cui l'utente seguito è offline gli verrà comunicato al suo login
                 */
                try {
                    notificationService.updateFollow(idUser, this.utente.getNome(), followers);
                } catch (RemoteException e) {
                    System.out.println("[ERRORE: Servizio Callback: Callback updateFollow ]");
                }
                return risp;
            }

        } catch (NullPointerException e){
            risp = "Il nome dell'utente non può essere null!";
            return risp;
        }

    }

    /**
     * L’utente chiede di non seguire più l’utente che ha per username idUser.
     * @param idUser nome dell'utente che si vuole smettere di seguire
     * @return una stringa contenetente il messaggio di risposta
     */
    private String unfollowUser(String idUser) {
        String risp;

        try{
            if (utenti.get(idUser) == null) {
                risp = "Errore. " + idUser + " non è un utente di Winsome, non puoi smettere di seguirlo!";
                return risp;
            } else if (this.utente.getNome().equals(idUser)) {
                risp = "Errore, non puoi smettere di seguirti da solo!";
                return risp;
            } else {
                Utente u = utenti.get(idUser);

                if(followers.get(u.getNome()).remove(this.utente.getNome())){
                    risp = "Hai smesso di seguire " + idUser + ".\r\nVerranno rimossi dal tuo feed tutti i post di "+ idUser + "!";
                } else{
                    risp = "Errore. Non segui l'utente " + idUser + "!\nPer smettere di seguire un utente devi prima seguirlo!";
                    return risp;
                }

                /*
                 * L'utente smette di seguire l'altro utente, si notifica al client(utente seguito), se online, tramite callbacks.
                 * Nel caso in cui l'utente seguito è offline gli verrà comunicato al suo login
                 */
                try {
                    notificationService.updateUnfollow(idUser, this.utente.getNome(), followers);
                } catch (RemoteException e) {
                    System.out.println("> Servizio Callback: Callback update unfollow error");
                }
                return risp;
            }
        } catch (NullPointerException e){
            risp = "Il nome dell'utente non può essere null!";
            return risp;
        }

    }

    /**
     * Operazione per recuperare la lista dei post di cui l’utente è autore. Viene restituita una lista dei post
     * presenti nel blog dell’utente. Per ogni post viene fornito id del post, autore e titolo.
     * Il modo in cui è strutturato è uguale al metodo showFeed.
     * @return una stringa contente la lista con i post dell'utente
     */
    private String viewBlog() {
        String risp = "Post prensenti nel tuo blog: ";

        for (Post copia: this.utente.getListaPosts()) {
            risp += "\r\nIdPost: " + copia.getIdPost() + "\r\n\tAutore: " + copia.getAutore().getNome() + " \r\n\tTitolo: " + copia.getTitolo();

        }
        return risp;

    }

    /**
     * Operazione per pubblicare un nuovo post. L’utente deve fornire titolo e contenuto del post. Il titolo ha lunghezza
     * massima di 20 caratteri e il contenuto una lunghezza massima di 500 caratteri. Se l’operazione va a buon fine, il
     * post è creato e disponibile per i follower dell’autore del post.
     * Il sistema assegna un identificatore univoco a ciascun post creato (idPost).
     * @param titolo stringa contente il titolo
     * @param contenuto stringa contentente il contenuto <= 500 caratteri
     * @return una stringa contente il post appena scritto
     */
    private String createPost(String titolo, String contenuto) {
        Utente utente = this.utente;
        int idPost = idP.incrementAndGet();
        Post post = new Post(utente, idPost, titolo, contenuto);

        if (posts.putIfAbsent(idPost, post) == null){
            utente.setListaPosts(post);
            return "Post creato: " + "\r\n\tidPost: " + post.getIdPost() + "\r\n\tTitolo: " + post.getTitolo() + "\r\n\tContenuto: " + post.getContenuto();
        }else {
            return "Errore, non puoi scrivere un post uguale ad un altro post";
        }

    }

    /**
     * Operazione per recuperare la lista dei post nel proprio feed. Viene restituita una lista dei post.
     * Per ogni post viene fornito id, autore e titolo del post.
     *
     * @return una stringa contenetente i post presenti nel proprio feed
     */
    private String showFeed() {
        String risp = "Lista dei post presenti nel tuo feed: ";
        Utente utente = this.utente;
        List<String> listFollowing = new LinkedList<>();

        for (String u: followers.keySet()) {
            for (String nome: followers.get(u)) {
                if (utente.getNome().equals(nome)){
                    listFollowing.add(u);
                }
            }
        }

        for (String s: listFollowing) {

            for (Post p: utenti.get(s).getListaPosts()) {
                risp += "\r\nIdPost: " + p.getIdPost() + "\r\n\tAutore: " + p.getAutore().getNome() + " \r\n\tTitolo: " + p.getTitolo();
            }
        }

        return risp;

    }

    /**
     * Il server restituisce titolo, contenuto, numero di voti positivi, numero di voti negativi e commenti del post.
     * Se l’utente è autore del post può cancellare il post con tutto il suo contenuto associato (commenti e voti).
     * Se l’utente ha il post nel proprio feed può esprimere un voto, positivo o negativo (solo un voto, successivi
     * tentativi di voto non saranno accettati dal server, che restituirà un messaggio di errore) e/o inserire un commento.
     *
     * @param idPost id del Post che si vuole visualizzare
     * @return una stringa contenetente titolo, contenuto, numero di voti positivi, numero di voti negativi e commenti del post
     */
    private String showPost(int idPost) {
        String risp;
        Utente u = this.utente;

        if(posts.get(idPost) == null){
            risp = "Errore, il post non è presente su Winsome, inserire un idPost valido";
            return risp;

        }else{
            Post p = posts.get(idPost);
            String post = "Titolo: " + p.getTitolo() + "\n\t" + p.getContenuto() + "\nNumero voti Positivi: " + p.getVotiPositivi() + "\r\nNumero voti Negativi: " + p.getVotiNegativi() + "\r\n" + p.getListaCommenti() ;

            Vector<Post> vPost = carimentoFeed();
            if (vPost.contains(p)){
                risp = post + "\nIl post fa parte del tuo feed, puoi dare un voto (positivo (+1) o negativo(-1) e/o inserire un commento"
                        + "\nPer votare il post digita rate <idPost> <vote>" + "\r\nPer commentare digita comment <idPost> <comment>";
                return risp;

            }else if (u.getListaPosts().contains(p)){
                risp = post + "\nPuoi cancellare il post digitando delete <idPost>. Cancellando il post cancellerai anche tutti i commenti e i voti, l'azione è irreversibile!";
                return risp;

            }else {
                risp = post;
                return risp;
            }
        }

    }

    /**
     * Operazione per cancellare un post. La richiesta viene accettata ed eseguita solo se l’utente è l’autore del post.
     * Il server cancella il post con tutto il suo contenuto associato (commenti e voti). Non vengono calcolate ricompense
     * “parziali”, ovvero se un contenuto recente (post, voto o commento) non era stato conteggiato nel calcolo delle
     * ricompense perché ancora il periodo non era scaduto, non viene considerato nel calcolo delle ricompense.
     *
     * @param idPost id del Post che si vuole cancellare
     * @return una stringa contenetente il messaggio di risposta
     */
    private String deletePost(int idPost) {
        String risp;
        Utente utente = this.utente;

            if(posts.get(idPost) == null){
                risp = "Errore, il post non è presente su Winsome, inserire un idPost valido!";
                return risp;
            } else {
                Post p = posts.get(idPost);
                if (p.getAutore().getNome().equals(utente.getNome())){
                    utente.getListaPosts().remove(p);
                    posts.remove(idPost, p);
                    List<String> listUtenti = new CopyOnWriteArrayList<>(utenti.keySet());
                    for (String nome : listUtenti){
                        utenti.get(nome).getListaPosts().remove(p);
                    }
                    risp = "Post cancellato, il post non sarà più visibile sul proprio feed e nel feed degli altri utenti!";

                }else{
                    risp = "Errore, non puoi cancellare un post che non sia il tuo!";
                }
            }

        return risp;

    }

    /**
     * Operazione per effettuare il rewin di un post, ovvero per pubblicare nel proprio blog un post presente nel proprio feed.
     *
     * @param idPost id del post di cui fare il rewin
     * @return una stringa contenetente il messaggio di risposta
     */
    private String rewinPost(int idPost) {
        String risp;
        Utente u = this.utente;

        if (!posts.containsKey(idPost)){
            risp = "Errore, il post non è presente su Winsome, inserire un idPost valido!";
            return risp;
        } else {
            Post p = posts.get(idPost);
            if (p.getAutore().equals(u)){
                risp = "Errore, non puoi fare il rewin di un tuo stesso post!";
                return risp;

            } else if(!carimentoFeed().contains(p)){
                risp = "Errore, non puoi fare il rewin di un tuo stesso post!";
                return risp;
            }else {
                if (u.getListaPosts().contains(p)){
                    risp = "Errore, puoi fare il rewin dello stesso un post al più una volta!";
                    return risp;

                }else {
                    u.setListaPosts(p);
                    risp = "Il post ora è visibile nel tuo feed e sarà visibili a tutti i tuoi followers!";
                }
            }
        }
        return risp;

    }

    /**
     * Operazione per assegnare un voto positivo o negativo ad un post. Se l’utente ha il post nel proprio feed e
     * non ha ancora espresso un voto, il voto viene accettato, negli altri casi (ad es. ha già votato il post, non ha
     * il post nel proprio feed, è l’autore del post) il voto non viene accettato e il server restituisce un messaggio di errore.
     *
     * @param idPost id del post da votare
     * @param voto voto da dare al post +1 voto positivo, -1 voto negativo
     * @return una stringa contenetente il messaggio di risposta
     */
    private String ratePost(int idPost, int voto) {
        String risp;
        Utente u = this.utente;

        if (!posts.containsKey(idPost)) {
            risp = "Errore, il post non è presente su Winsome, inserire un idPost valido";
            return risp;

        } else {
            Post p = posts.get(idPost);
            if (p.getAutore().equals(u)) {
                risp = "Errore, non puoi votare un tuo stesso post";
                return risp;
            }
            Vector<Post> vPost = carimentoFeed();
            if (!vPost.contains(p)) {
                risp = "Errore, non puoi votare un post non presente nel tuo feed!";
                return risp;

            } else {

                if (p.getUtentiVoti().contains(u.getNome())) {
                    risp = "Errore, non puoi votare più volte lo stesso post!";
                    return risp;

                } else {
                    if (voto == (-1)) {
                        p.setVotiNegativi();
                        p.setListaLike(voto);
                        risp = "Hai aggiunto un voto negativo al post!";
                        return risp;

                    } else if (voto == (1)) {
                        p.setVotiPositivi();
                        p.setUtentiVoti(u); //solo l'utente che ha messo voti postivi viene conteggiato per il calcolo del guadagno
                        p.setListaLike(voto);
                        risp = "Hai aggiunto un voto positivo al post!";
                        return risp;

                    } else {
                        risp = "Errore! Gli unici valori ammissibili per votare sono (+1) per un voto positivo e (-1) per un voto negativo!";
                        return risp;
                    }
                }
            }
        }

    }

    /**
     * Operazione per aggiungere un commento ad un post. Se l’utente ha il post nel proprio feed, il commento viene
     * accettato, negli altri casi (ad es. l’utente non ha il post nel proprio feed oppure è l’autore del post) il
     * commento non viene accettato e il server restituisce un messaggio di errore.
     * Un utente può aggiungere più di un commento ad un post.
     *
     * @param idPost id del post a cui si vuole aggiungere un commento
     * @param commento stringa contente il commento da aggiungere al post
     * @return una stringa contenetente il messaggio di risposta
     */
    private String addComment(int idPost, String commento) {
        String risp;
        Utente u = this.utente;

        if (!posts.containsKey(idPost)){
            risp = "Errore, il post non è presente su Winsome, inserire un idPost valido!";
            return risp;
        } else {
            Post p = posts.get(idPost);
            if (p.getAutore().equals(u)){
                risp = "Errore, non puoi commentare un tuo stesso post!";
                return risp;
            }
            Vector<Post> vPost = carimentoFeed();
            if(!vPost.contains(p)) {
                risp = "Errore, non puoi commentare un post non presente nel tuo feed!";
                return risp;

            } else {
                p.setCommenti(this.utente.getNome(), commento);
                //p.setListaUtentiCommenti(u);
                p.setTabellaCommentiUtenti(u); // in questa HashMap si tiene aggiornato l'utente con il numero di commenti che fa sullo stesso post
                risp = "Commento aggiunto al post!";

            }
        }
        return risp;

    }

    /**
     * Operazione per recuperare il valore del proprio portafoglio.
     * Il server restituisce il totale e la storia delle transazioni (ad es. <incremento> <timestamp>)
     * @return una stringa contenetente il messaggio di risposta
     */
    private String getWallet() {
        String risp;
        Utente utente = this.utente;

        risp = "Guadagno totale attuale: " + utente.getGuadagnoTotale() + " wincoin!";
        List<String> listaTransazioni = new CopyOnWriteArrayList<>(utente.getListaTransazioni());
        risp = risp + "\r\nStoria delle transazioni: " + listaTransazioni;

        return risp;

    }

    /**
     * Operazione per recuperare il valore del proprio portafoglio convertito in bitcoin. Il server utilizza il servizio
     * di generazione di valori random decimali fornito da RANDOM.ORG per ottenere un tasso di cambio casuale e quindi
     * calcola la conversione.
     * @return una stringa contenetente il messaggio di risposta
     */
    private String getWalletInBitcoin() throws ExecutionException, InterruptedException {
        String risp;
        double bitcoinGuadagno;

        //utilizzo del Servizio Random.ORG
        Callable<Double> v = new RichiestaRandomORG();
        FutureTask<Double> futureTask = new FutureTask<>(v);
        Thread t=new Thread(futureTask);
        t.start();
        Double valuta = futureTask.get(); // will wait for the async completion

        bitcoinGuadagno = utente.getGuadagnoTotale() * valuta;
        bitcoinGuadagno = Math.round((bitcoinGuadagno * 100.0)) / 100.0;

        risp = "Ecco il valore del tuo portafoglio convertito in bitcoin, con il tasso di cambio attuale(" + valuta + "): ";
        risp = risp + "\r\t\nGuadagno totale in bitcoin: " + bitcoinGuadagno + " = " + utente.getGuadagnoTotale() + "*" + valuta;

        return risp;
    }


    //*************************************Metodi di supporto*********************************************************//

    /**
     * carimento post feed
     */
    private Vector<Post> carimentoFeed(){
        Utente utente = this.utente;
        List<String> listFollowing = new LinkedList<>();
        Vector<Post> feed = new Vector<>();

        for (String u: followers.keySet()) {
            for (String nome: followers.get(u)) {
                if (utente.getNome().equals(nome)){
                    listFollowing.add(u);
                }
            }
        }

        for (String s: listFollowing) {
            feed.addAll(utenti.get(s).getListaPosts());
        }
        return feed;

    }

}