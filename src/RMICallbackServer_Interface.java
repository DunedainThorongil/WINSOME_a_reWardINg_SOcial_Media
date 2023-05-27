import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMICallbackServer_Interface extends Remote {

    /**
     * Metodo usato dal Client per registrasi ad un evento
     * Passando come argomento ClientInterface il Server riesce a reperire lo stub del Client
     * @param ClientInterface interfaccia Remota del Client che fa la richiesta.
     * @throws RemoteException essendo un oggetto remoto può lanciare questa eccezione.
     */
     void registrazioneCallback (String s1, RMICallbackClient_Interface ClientInterface) throws RemoteException;

    /**
     * Metodo per rimuovere la registrazione del cliente
     * @param ClientInterface cliente che vuole disiscriversi dal servizio di notifica
     * @throws RemoteException essendo un oggetto remoto può lanciare questa eccezione.
     */
     void rimuoviRegistrazioneCallback(String s1, RMICallbackClient_Interface ClientInterface) throws RemoteException;


}
