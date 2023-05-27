import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/*
    Classe che modella un post all'interno del sistema con tutte le sue caratteristiche richieste dalle
    specifiche.
    La classe implementa Serializable perchè le informazioni devono essere persistenti.
*/
public class Post {

    private final Utente autore;
    private final int idPost;
    private final String titolo;
    private final String contenuto;
    private final AtomicInteger votiPositivi;
    private final AtomicInteger votiNegativi;
    private final AtomicInteger nValutazioni; // numero di quante volte è stato valutato il post per il calcolo del guadagno

    private final ConcurrentHashMap<String, Vector<String>> listaCommenti; // lista dei commenti ricevuti dagli utenti
    private Vector<Integer> listaLike; // lista dei like ricevuti dalle persone

    // Variabili usate per il calcolo del guadagno dei post
    private ConcurrentHashMap<String, AtomicInteger> tabellaCommentiUtenti; //Tabella dei Utenti che hanno commentato il post <Utente, nCommenti>
    private Vector<String> listaUtentiCommenti; // lista degli utenti che hanno commentato
    private Vector<String> utentiVoti; // lista degli utenti che hanno votato

    public Post(Utente autore, int idPost, String titolo, String contenuto) {

        this.autore = autore;
        this.idPost = idPost;
        this.titolo = titolo;
        this.contenuto = contenuto;
        votiPositivi = new AtomicInteger(0);
        votiNegativi = new AtomicInteger(0);
        nValutazioni = new AtomicInteger(0);

        listaCommenti = new ConcurrentHashMap<>();
        tabellaCommentiUtenti = new ConcurrentHashMap<>();
        listaUtentiCommenti = new Vector<>();
        utentiVoti = new Vector<>();

        listaLike = new Vector<>();

    }

    // METODI GET
    public int getIdPost() {
        return idPost;
    }

    public String getContenuto() {
        return contenuto;
    }

    public String getTitolo() {
        return titolo;
    }

    public Utente getAutore() {
        return autore;
    }

    public int getVotiPositivi() {
        return votiPositivi.get();
    }

    public int getVotiNegativi() {
        return votiNegativi.get();
    }

    public int getnValutazioni() {
        return nValutazioni.get();
    }

    public Vector<String> getUtentiVoti(){
        if (utentiVoti == null){
            utentiVoti = new Vector<>();
        }
        return utentiVoti;
    }

    public String getListaCommenti(){
        StringBuilder risp = new StringBuilder("Commenti: ");

        if (listaCommenti.isEmpty()){
            risp.append("Non ci sono ancora commenti");

        } else {
            for (String s: listaCommenti.keySet()) {
                Vector<String> lis = new Vector<>(listaCommenti.get(s));
                risp.append("\t\n\t--").append(s).append(" ").append("ha scritto: ").append(lis).append(";");
            }

        }

        return risp.toString();

    }

    public ConcurrentHashMap<String, AtomicInteger> getTabellaCommentiUtenti(){
        if(tabellaCommentiUtenti == null){
            tabellaCommentiUtenti = new ConcurrentHashMap<>();
        }
        return tabellaCommentiUtenti;
    }

    public Vector<String> getListaUtentiCommenti(){
        if (listaUtentiCommenti == null){
            listaUtentiCommenti = new Vector<>();
        }
        return listaUtentiCommenti;
    }

    public Vector<Integer> getListaLike(){
        if (listaLike == null){
            listaLike = new Vector<>();
        }
        return listaLike;
    }

    //METODI SET
    public void setVotiPositivi(){
        votiPositivi.getAndIncrement();
    }

    public void setVotiNegativi(){
        votiNegativi.getAndIncrement();
    }

    public void setCommenti(String username, String commento){

        listaCommenti.putIfAbsent(username, new Vector<>());
        listaCommenti.get(username).add(commento);
    }

    public void setTabellaCommentiUtenti(Utente utente){
        if (tabellaCommentiUtenti == null){
            tabellaCommentiUtenti = new ConcurrentHashMap<>();
        }

        if (tabellaCommentiUtenti.putIfAbsent(utente.getNome(), new AtomicInteger(1)) != null){
            tabellaCommentiUtenti.get(utente.getNome()).getAndIncrement();
        } else {
            listaUtentiCommenti.add(utente.getNome());
        }

    }

    public void setUtentiVoti(Utente u){
        if (utentiVoti == null){
            utentiVoti = new Vector<>();
        }

        if (!utentiVoti.contains(u.getNome())) {
            utentiVoti.add(u.getNome());
        }
    }

    public void setListaLike(int voto){
        listaLike.add(voto);
    }

}
