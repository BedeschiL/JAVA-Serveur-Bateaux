/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocoleCSA;

import ProtocoleCSA.ReponseCSA;
import beansForJdbc.BeanBDAccess;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.Vector;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequeteCSA implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGINA = 1;
    public static int LCLIENTS = 2;
    public static int STOP = 3;
    
    private int type;
    private String chargeUtile;
    private Socket socketClient;
    private ObjectInputStream ois;

    public RequeteCSA(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }
    
    public RequeteCSA(int t, String chu, Socket s) {
        type = t;
        chargeUtile = chu;
        socketClient = s;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }

    public int getType() {
        return type;
    }

    public ObjectInputStream getOis() {
        return ois;
    }

    public void setOis(ObjectInputStream ois) {
        this.ois = ois;
    }
    
    @Override
    public Runnable createRunnable(Socket s, ConsoleServeur cs) {
        if(type == LOGINA) {
            return new Runnable() {
                public void run() {
                    traiteRequeteLogin(s, cs);
                }
            };
        }
        else {
            return new Runnable() {
                public void run() {
                    traiteRequeteLoggedOut(s, cs);
                }
            };
        }
    }
    
    private void traiteRequeteLogin(Socket sock, ConsoleServeur cs) {
        BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_mouvements", "hector", "WA0UH.nice.key");
        try {
            db.creerConnexionBD();
        }
        catch (Exception ex) {
            return;
        }
        
        boolean loggedIn = false;
        
        ObjectOutputStream oos = null;
        RequeteCSA req = null;
        ReponseCSA rep = null;
        
        try {
            oos = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException ex) {
            System.err.println("Erreur ? [" + ex.getMessage() + "]");
        }
        
        while(true) {
            if(req.getType() == RequeteCSA.LOGINA) {
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de LoginA : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = getChargeUtile();
                
                if(!loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String user = parser[0];
                        String pass = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Connexion de " + user + "; MDP = " + pass + "#" + Thread.currentThread().getName());
                        ResultSet rs;
                        try {
                            rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + user + "'");
                            if(rs.next() && pass.equals(rs.getString("pass"))) {
                                loggedIn = true;
                                rep = new ReponseCSA(ReponseCSA.LOGINA_OK, null);
                            }
                            else
                                rep = new ReponseCSA(ReponseCSA.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponseCSA(ReponseCSA.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseCSA(ReponseCSA.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseCSA(ReponseCSA.ALREADY_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteCSA.LCLIENTS) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de LClients : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    
                }
                else
                    rep = new ReponseCSA(ReponseCSA.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteCSA.STOP) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Stop : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 1) {
                        String temps = parser[0];
                        cs.TraceEvenements(adresseDistante + "#Shutdown en " + temps + " secondes#" + Thread.currentThread().getName());

                        
                    }
                    else
                        rep = new ReponseCSA(ReponseCSA.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseCSA(ReponseCSA.NOT_LOGGED_IN, null);
            }
            else
                rep = new ReponseCSA(ReponseCSA.UNKNOWN_TYPE, null);

            try {
                oos.writeObject(rep);
                oos.flush();
                
                req = (RequeteCSA)ois.readObject();
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
            }
            catch (Exception e) {
                System.err.println("Erreur ? [" + e.getMessage() + "]");
                break;
            }
        }
        
        try {
            sock.close();
        } catch (IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
    
    private void traiteRequeteLoggedOut(Socket sock, ConsoleServeur cs) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            oos.writeObject(new ReponseCSA(ReponseCSA.NOT_LOGGED_IN, null));
            oos.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
}
