import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface RMICallbackClient_Interface extends Remote {

    /**
     * Metodo usato dal Server per comunicare al Client il cambiamento di stato dell'ogetto di cui ha registrato l'interesse
     * nel nostro caso il CLient vuole ricevere una notifica che gli comunica il follow di un untente.
     * La notifica dell'evento aggiorna la struttura locale del Client.
     * Viene presa una nuova struttura dati aggiornata e viene copiata in quella locale.
     *
     * @param utenteSeguito utente che viene seguito
     * @param utenteCheFaAzione nome utente che fa l'azione di iniziare a seguire
     * @param followers hashMap di followers
     * @throws RemoteException essendo un oggetto remoto può lanciare questa eccezione
     */
    void notificaFollow(String utenteSeguito, String utenteCheFaAzione, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException;

    /**
     * Metodo uguale a quello di sopra, qui viene mandata una notifica dell'unfollow
     * @param utenteSeguito nome dell'utente che subisce l'azione
     * @param utenteCheFaAzione nome dell'utente che fa l'azione
     * @param followers hashMap di followers
     * @throws RemoteException essendo un oggetto remoto può lanciare questa eccezion
     */
    void notificaUnfollow(String utenteSeguito, String utenteCheFaAzione, ConcurrentHashMap <String, ConcurrentLinkedQueue<String>> followers) throws RemoteException;

    /**
     * Metodo che aggiorna i followers al login del client
     * @param followers hashMaps contenente i followers
     * @throws RemoteException essendo un oggetto remoto può lanciare questa eccezzione
     */
    void notificaUpadateFollowers(ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException;
}
