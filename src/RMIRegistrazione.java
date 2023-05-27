import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
 * Classe che modella l'operazione di registrazione di un utente alla piattaforma tramite il meccanismo di RMI
 */
public class RMIRegistrazione extends RemoteServer implements RMIRegistrazione_Interface {

    @Serial
    private static final long serialVersionUID = 1L;
    // HashMap utulizzata per memorizzare gli utenti che si registrano
    private final ConcurrentHashMap<String, Utente> utentiRegistrati;
    //HashMap contente il nome e la lista dei suoi followers, quando un utente si registra questa lista è vuota
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers;

    public RMIRegistrazione(ConcurrentHashMap<String, Utente> utentiRegistrati, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers){
        super();
        this.utentiRegistrati = utentiRegistrati;
        this.followers = followers;
    }

    @Override
    public String registrazione(String username, String password, Vector<String> t) throws RemoteException {

        if (utentiRegistrati.get(username) != null) {
            return "Utente già registrato con questo nome!";
        }
        if (username.isBlank()){
            return "Il nome utente non può essere uno spazio bianco o una sequenza di spazi bianchi!";
        }
        if (password.isBlank()){
            return "La password non può essere uno spazio bianco o una sequenza di spazi bianchi!";
        }
        if (t.size() > 5){
            return "Errore, inseriti troppi tags, se ne possono inserire massimo 5!";
        }

        Vector<String> tags = new Vector<>();
        for (int i = 0; i < t.size() - 1; i++){
            String []ta = t.get(i).split(",");
            if (ta[0].isBlank()){
                return "Errore, i tags non possono essere uno spazio bianco!";
            }
            tags.add(ta[0]);
        }
        tags.add(t.lastElement());

        Utente u = new Utente(username, password, tags);
        utentiRegistrati.putIfAbsent(u.getNome(), u); // 'putIfAbset' a differenza di 'put' è un metodo atomico
        followers.putIfAbsent(username, new ConcurrentLinkedQueue<>());

        //Salvataggio degli utenti registrati sul file JSON RegistroUtenti.json
        WinsomeServerMain.salvaUtente();
        //Salvataggio dei post registrati sul file JSON RegistroPost.json
        WinsomeServerMain.salvaPosts();
        //Salvataggio dei followers registrati sul file JSON RegistroFollowers.json
        WinsomeServerMain.salvaFollowers();

        return u.getNome() + " iscritto a Winsome!" +
                "\nBenvenuto su Winsome, \nper iniziare a interagire con gli" +
                " altri utenti digita <list users> per visualizzare gli utenti con cui hai interessi in comune!\n" +
                "Quando inizierai a seguire un utente potrai vedere nel tuo feed tutti i suoi post, potrai commentarli, " +
                "votarli e fare il rewin!"+ "\nBuon divertimento su Winsome!";
    }

}

