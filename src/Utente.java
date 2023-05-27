import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.lang.Math.*;
/*
    Classe che modella un utente all'interno del sistema con tutte le sue caratteristiche richieste dalle
    specifiche.
    La classe implementa Serializable perchè le informazioni devono essere persistenti.
    */
public class Utente {

    private final String nome;
    private final String password;
    private final Vector<String> tags;

    private transient ConcurrentLinkedQueue<Post> listaPosts; // Lista dei post dell'utente di cui è autore
    private final Vector<String> listaTransazioni; //lista delle Transazioni

    private double ricompensaAutore;
    private double ricomensaCuratore;
    private double guadagnoTot;
    private double incrementoGuadagno;
    private boolean stato; // l'utente può trovarsi in due stati se è true è online altrimenti è offline

    // costruttore
    Utente(String nome, String password , Vector<String> tags){
        this.nome = nome;
        this.password = password;
        this.tags = tags;

        listaPosts = new ConcurrentLinkedQueue<>();
        listaTransazioni = new Vector<>();
        //portafoglio
        ricompensaAutore = 0.00;
        ricomensaCuratore = 0.00;
        guadagnoTot = 0.00;
        incrementoGuadagno = 0.00;
        stato = false; //offline

    }

    // METODI GET

    public String getNome() {
        return nome;
    }

    public String getPassword(){return password;}

    public Vector<String> getTags() {
        return tags;
    }

    public ConcurrentLinkedQueue<Post> getListaPosts(){
        if (listaPosts == null){
            listaPosts = new ConcurrentLinkedQueue<>();
        }
        return listaPosts;
    }

    public Vector<String> getListaTransazioni(){
        return listaTransazioni;
    }

    public double getIncrementoGuadagno(){
        return Math.round(incrementoGuadagno * 100.0)/ 100.0;
    }

    public double getGuadagnoTotale(){
        guadagnoTot += ricompensaAutore + ricomensaCuratore;
        return Math.round(guadagnoTot * 100.0)/ 100.0;
    }

    // METODI SET

    public void setStato (boolean s){ this.stato = s; }

    public void setListaPosts(Post p){
        if (listaPosts == null){
            listaPosts = new ConcurrentLinkedQueue<>();
        }
        if (!listaPosts.contains(p)){
            listaPosts.add(p);
        }

    }

    public void setIncrementoGuadagno(double autore, double curatore){
        incrementoGuadagno = autore + curatore;

        ricompensaAutore += autore;
        ricomensaCuratore += curatore;
    }

    public void setListaTransazioni(String data){
        this.listaTransazioni.add(data);
    }

    //Metodi per il calcolo del Portafoglio
    public double calcolaGuadagnoPost(Post post) {

        double guadagno = 0.00;

        Vector<Integer> listaLike = new Vector<>(post.getListaLike());
        Vector<String> listaUtenteCom = new Vector<>(post.getListaUtentiCommenti());

        if (!listaLike.isEmpty() && !listaUtenteCom.isEmpty()) {

            int cp; //indica il numero di commenti che una certa persona p ha fatto
            int n; // numero iterazioni

            //calcolo il numero di iterazioni
            n = post.getnValutazioni() + 1;
            int sommatoria1 = 0;
            double sommatoria2 = 0;
            int p = 0;

            //calcolo sommatoria1
            while (p < listaLike.size()) {
                int i = listaLike.get(p);
                if (i == 1) {
                    sommatoria1++;
                } else {
                    sommatoria1--;
                }
                p++;
            }

            post.getListaLike().clear(); //svuoto la lista in modo che la prossima volta che calcolo il posto conto solo i like nuovi

            for (String nome : listaUtenteCom) {
                cp = post.getTabellaCommentiUtenti().get(nome).get();
                sommatoria2 += (2 / (1 + Math.exp(-(cp - 1))));
            }

            post.getListaUtentiCommenti().clear(); //svuoto la lista in modo che la prossima volta che calcolo il guadagno del post conto solo i commenti nuovi
            post.getTabellaCommentiUtenti().clear();

            //calcolo max
            int max = max(sommatoria1, 0);

            //calcolo dei logartimi
            double log1 = Math.log(max + 1);
            double log2 = Math.log(sommatoria2 + 1);

            //calolo finale sul guadagno del post
            guadagno = (log1 + log2) / n;
            //System.out.println("Il guadagno è:" + guadagno);
            return guadagno;
        }

        return guadagno;

    }


}
