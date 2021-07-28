/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocolePFMCOP;

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
public class RequetePFMCOP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGIN_GROUP = 1;
    public static int POST_QUESTION = 2;
    public static int ANSWER_QUESTION = 3;
    public static int POST_EVENT = 4;
    
    private int type;
    private String chargeUtile;
    private Socket socketClient;
    private ObjectInputStream ois;

    public RequetePFMCOP(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }
    
    public RequetePFMCOP(int t, String chu, Socket s) {
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
        if(type == LOGIN_GROUP) {
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
        RequetePFMCOP req = null;
        ReponsePFMCOP rep = null;
        
        try {
            oos = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException ex) {
            System.err.println("Erreur ? [" + ex.getMessage() + "]");
        }
        
        while(true) {
            if(req.getType() == RequetePFMCOP.LOGIN_GROUP) {
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login_Group : adresse distante = " + adresseDistante);
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
                                rep = new ReponsePFMCOP(ReponsePFMCOP.LOGIN_GROUP_OK, null);
                            }
                            else
                                rep = new ReponsePFMCOP(ReponsePFMCOP.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponsePFMCOP(ReponsePFMCOP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponsePFMCOP(ReponsePFMCOP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponsePFMCOP(ReponsePFMCOP.ALREADY_LOGGED_IN, null);
            }
            else if(req.getType() == RequetePFMCOP.POST_QUESTION) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Post_Question : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String identifiant = parser[0];
                        String question = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Pose question " + identifiant + " : " + question + "#" + Thread.currentThread().getName());

                        
                    }
                    else
                        rep = new ReponsePFMCOP(ReponsePFMCOP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponsePFMCOP(ReponsePFMCOP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequetePFMCOP.ANSWER_QUESTION) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début d'Answer_Question : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String identifiant = parser[0];
                        String reponse = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Réponse question " + identifiant + " : " + reponse + "#" + Thread.currentThread().getName());

                        
                    }
                    else
                        rep = new ReponsePFMCOP(ReponsePFMCOP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponsePFMCOP(ReponsePFMCOP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequetePFMCOP.POST_EVENT) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Post_Event : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String identifiant = parser[0];
                        String evenement = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Signale événement " + identifiant + " : " + evenement + "#" + Thread.currentThread().getName());

                        try {
                            req = (RequetePFMCOP)ois.readObject();
                            System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
                        }
                        catch (ClassNotFoundException e) {
                            System.err.println("Erreur de def de classe [" + e.getMessage() + "]");
                        }
                        catch (IOException e) {
                            System.err.println("Erreur ? [" + e.getMessage() + "]");
                            break;
                        }
                        
                        continue;
                    }
                    else
                        rep = new ReponsePFMCOP(ReponsePFMCOP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponsePFMCOP(ReponsePFMCOP.NOT_LOGGED_IN, null);
            }
            else
                rep = new ReponsePFMCOP(ReponsePFMCOP.UNKNOWN_TYPE, null);

            try {
                oos.writeObject(rep);
                oos.flush();
                
                req = (RequetePFMCOP)ois.readObject();
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
            oos.writeObject(new ReponsePFMCOP(ReponsePFMCOP.NOT_LOGGED_IN, null));
            oos.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
}
