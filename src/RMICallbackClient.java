import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe con l'implementazione dei metodi dichiarati nell'interfaccia RMICallbackClient_Interface
 */
public class RMICallbackClient extends RemoteObject implements RMICallbackClient_Interface{
    @Serial
    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers;
    private final ConcurrentLinkedQueue<String> utentiOnline;

    //costruttore
    public RMICallbackClient(ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers, ConcurrentLinkedQueue<String> utentiOnline) throws RemoteException{
        super();
        this.followers = followers;
        this.utentiOnline = utentiOnline;
    }

    @Override
    public void notificaFollow(String utenteSeguito, String utenteChefaAzione, ConcurrentHashMap <String, ConcurrentLinkedQueue<String>> followers) throws RemoteException {

        this.followers.putAll(followers);
        if (utentiOnline.contains(utenteSeguito)){
            System.out.print("[ Notifica: " + utenteChefaAzione + " ha iniziato a seguirti! ]\n\n> ");
        }

    }

    @Override
    public void notificaUnfollow(String utenteSeguito, String utenteCheFazione, ConcurrentHashMap <String, ConcurrentLinkedQueue<String>> followers) throws RemoteException {

        this.followers.putAll(followers);
        if (utentiOnline.contains(utenteSeguito)){
            System.out.print("[ Notifica: " + utenteCheFazione + " ha smesso di seguirti! ]\n\n> ");
        }

    }

    @Override
    public void notificaUpadateFollowers(ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException {
        this.followers.putAll(followers);
    }

}
