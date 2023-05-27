import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

/*
 * Mediante il sistema di RMI il server mette a disposizione del client la possibilità di richiamare un metodo per
 * effettuare la registrazione al sistema.
 * Il Client riceve un messaggio che indica l'esito dell'operazione.
 */
public interface RMIRegistrazione_Interface extends Remote {
    /**
     *
     * @param username nome dell'utente
     * @param password password dell'utente
     * @param tags lista di tags dell'utente
     * @throws RemoteException Ogni metodo di una interfaccia remota deve avere RemoteException
     */
    String registrazione(String username, String password, Vector<String> tags) throws RemoteException;

}
