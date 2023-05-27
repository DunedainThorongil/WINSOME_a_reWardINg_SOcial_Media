import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe che modella il task che calcolerà ogni 4 minuti il guadagno per ogni utente, e poi manderà il messaggio ai client iscritti al gruppo
 */
public class TaskServerMulticast implements Runnable {

    private final String multicast;
    private final int mcastpost;
    private final ConcurrentHashMap<String, Utente> utenti;
    private final int attesa;
    private final int percentualeAutore;

    public TaskServerMulticast(String multicast, int mcastport, ConcurrentHashMap<String, Utente> utenti, int attesa, int percentualeAutore) {
        this.multicast = multicast;
        this.mcastpost = mcastport;
        this.utenti = utenti;
        this.attesa = attesa;
        this.percentualeAutore = percentualeAutore;
    }

    @Override
    public void run() {

        while (true){

            try {
                Thread.sleep(attesa);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                InetAddress inetAddress = InetAddress.getByName(multicast);

                String messaggio = "[ Notifica gruppo Multicast: calcolo guadagno avvenuto, digita <wallet> o <wallet btc> " +
                        "per visualizzare il tuo portafoglio! ]\r\n> ";

                byte[] data = messaggio.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, mcastpost);

                calcoloGuadagno(utenti, percentualeAutore);
                datagramSocket.send(datagramPacket);

                System.out.println("[ Multicast: messaggio mandato agli iscritti al gruppo multicast! ]");

            } catch (IOException e ){
                e.printStackTrace();
                return;
            }
        }

    }

    private void calcoloGuadagno(ConcurrentHashMap<String, Utente> utenti, int percentualeAutore) {
        SimpleDateFormat data = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss");
        String time = data.format(new Date());

        Vector<Utente> listaUtenti = new Vector<>(utenti.values());
        Vector<Post> listaPost = new Vector<>();
        Vector<Utente> listaCuratori = new Vector<>();

        double guadagno;
        double ricompensaAutore;
        double ricompensaCuratore;

        for (Utente utente: listaUtenti) {
            listaPost.addAll(0, utente.getListaPosts());
            for (Post p : listaPost) {
                if (p.getAutore().getNome().equals(utente.getNome())){ // se un utente fa un rewin di un post non prende i suoi guadagni
                    guadagno = utente.calcolaGuadagnoPost(p);
                    assegnamentoListaCuratori(p, listaCuratori);
                    ricompensaAutore = (guadagno * percentualeAutore) / 100;
                    ricompensaCuratore = ( (guadagno * (100 - percentualeAutore)) / 100 ) / listaCuratori.size();
                    utente.setIncrementoGuadagno(ricompensaAutore, 0);

                    for (Utente u: listaCuratori) {
                        u.setIncrementoGuadagno(0, ricompensaCuratore);
                    }
                }
            }

            listaPost.clear(); // svuoto il vettore in cui andrò a mettere i post dell'utente successivo
        }

        for (Utente utente: listaUtenti) {
            utente.setListaTransazioni("$$$ Data: " + time + " " + "Incremento guadagno: " + utente.getIncrementoGuadagno() + " $$$");
        }
        WinsomeServerMain.salvaUtente(); // salvo le informazioni aggiornate sul file

    }

    private void assegnamentoListaCuratori(Post post, Vector<Utente> listaCuratori){
        Vector<String> listaUtentiLike = new Vector<>(post.getUtentiVoti());
        Vector<String> listautentiCommenti = new Vector<>(post.getListaUtentiCommenti());

        Vector<String> listaSenzaDuplicati = new Vector<>(listaUtentiLike);
        for (String nome: listautentiCommenti) {
            if (!listaSenzaDuplicati.contains(nome)){
                listaSenzaDuplicati.add(nome);
            }
        }

        for (String nome: listaSenzaDuplicati) {
            listaCuratori.add(utenti.get(nome));
        }

    }


}