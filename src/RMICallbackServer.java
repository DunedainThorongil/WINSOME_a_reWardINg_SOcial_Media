import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe con l'implementazione dei metodi dichiarati nell'interfaccia RMICallbackServer_Interface
 */
public class RMICallbackServer extends RemoteServer implements RMICallbackServer_Interface{

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * Struttura dati contente i clienti rergistrati al servizio di notifica
     */
    private final ConcurrentHashMap<String, RMICallbackClient_Interface> clienti;

    public RMICallbackServer() throws RemoteException{
        super();
        clienti = new ConcurrentHashMap<>();
    }

    @Override
    public void registrazioneCallback(String username, RMICallbackClient_Interface ClientInterface) throws RemoteException {

        if (!clienti.contains(ClientInterface)){
            clienti.putIfAbsent(username, ClientInterface);
            //System.out.println("Lista utenti iscritti: " + clienti.keySet());
            System.out.println("[ Servizio Callback: " +  username + "  registrato al servizio di notifica ]");
        }

    }

    @Override
    public void rimuoviRegistrazioneCallback(String username, RMICallbackClient_Interface ClientInterface) throws RemoteException{

        if (clienti.remove(username, ClientInterface)){
            System.out.println("[ Servizio Callback: "+ username + " rimosso dal servizio di notifica! ]");
            //System.out.println("Lista utenti iscritti(IF): " + clienti.keySet());
        }
    }

    public void updateFollow(String utenteSeguito, String utenteCheSegue, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException{
        doCallbacksFollow(utenteSeguito, utenteCheSegue, followers);
    }

    private void doCallbacksFollow(String utenteSeguito, String utenteCheSegue, ConcurrentHashMap <String, ConcurrentLinkedQueue<String>> followers) throws RemoteException{
        System.out.println("[ Servizio Callback: Inizio callbacks ]");
        if (clienti.get(utenteCheSegue) != null){
            try{
                RMICallbackClient_Interface client_interface = clienti.get(utenteSeguito);
                client_interface.notificaFollow(utenteSeguito, utenteCheSegue, followers);

            }catch (NullPointerException e ){ // nel caso in cui l'utente sia disconnesso quando viene seguito

                System.out.println("Cliente non loggato al momento del follow!");
            }
        }
        System.out.println("[ Servizio Callback: Callbacks completato! ]");

    }

    public void updateUnfollow(String utenteSeguito, String utenteCheSegue, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException{
        doCallBacksUnfollow(utenteSeguito, utenteCheSegue, followers);
    }

    private void doCallBacksUnfollow(String untenteSeguito, String utenteCheSegue, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException{

        System.out.println("[ Servizio Callbacks: Inizio callbacks ]");

       if (clienti.get(utenteCheSegue) != null){
           try{
               RMICallbackClient_Interface client_interface = clienti.get(untenteSeguito);
               client_interface.notificaUnfollow(untenteSeguito, utenteCheSegue, followers);
           }catch (NullPointerException e){
               System.out.println("Cliente non loggato al momento dell'unfollow!");
           }
       }
        System.out.println("[ Servizio Callbacks: Callbacks completato! ]");
    }

    public void updateFollowers(String username, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException {
        doCallBackUpdateFollowers(username, followers);

    }

    private void doCallBackUpdateFollowers(String username, ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> followers) throws RemoteException {
        RMICallbackClient_Interface utente = clienti.get(username);
        utente.notificaUpadateFollowers(followers);

    }

}
