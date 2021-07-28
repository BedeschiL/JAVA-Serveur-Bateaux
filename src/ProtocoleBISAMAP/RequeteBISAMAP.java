/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocoleBISAMAP;

import beansForJdbc.BeanBDAccess;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.Vector;
import javax.crypto.*;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequeteBISAMAP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static String codeProvider = "BC"; //CryptixCrypto";
    
    public static int LOGIN = 1;
    public static int GET_NEXT_BILL = 2;
    public static int VALIDATE_BILL = 3;
    public static int LIST_BILLS = 4;
    public static int SEND_BILLS = 5;
    public static int REC_PAY = 6;
    public static int LIST_WAITING = 7;
    
    private int type;
    private String chargeUtile;
    private byte[] digest;
    private Socket socketClient;
    private ObjectInputStream ois;

    public RequeteBISAMAP(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }
    
    public RequeteBISAMAP(int t, String chu, byte[] dig) {
        type = t;
        chargeUtile = chu;
        digest = dig;
    }
    
    
    public RequeteBISAMAP(int t, String chu, Socket s) {
        type = t;
        chargeUtile = chu;
        socketClient = s;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
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
        if(type == LOGIN) {
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
        RequeteBISAMAP req = this;
        ReponseBISAMAP rep = null;
        
        try {
            oos = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException ex) {
            System.err.println("Erreur ? [" + ex.getMessage() + "]");
        }
        
        while(true) {
            if(req.getType() == RequeteBISAMAP.LOGIN) {
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login : adresse distante = " + adresseDistante);
                // la charge utile est le nom
                String user = req.getChargeUtile();
                
                if(!loggedIn) {
                    cs.TraceEvenements(adresseDistante + "#Connexion de " + user + "#" + Thread.currentThread().getName());
                    ResultSet rs;
                    try {
                        rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + user + "'");
                        if(rs.next())
                        {
                            String pass = rs.getString("pass");

                            System.out.println("Recuperation de la cle secrète");
                            ObjectInputStream cleFichS = new ObjectInputStream(new FileInputStream("x.ser"));
                            SecretKey cle = (SecretKey)cleFichS.readObject();
                            cleFichS.close();
                            System.out.println(" *** Cle secrète récupérée = " + cle.toString());

                            // confection d'un hmac local
                            Mac hmac = Mac.getInstance("HMAC-MD5", RequeteBISAMAP.codeProvider);
                            hmac.init(cle);
                            hmac.update(pass.getBytes());
                            byte[] msgDLocal = hmac.doFinal();

                            if(msgDLocal.equals(req.getDigest())) {
                                loggedIn = true;
                                rep = new ReponseBISAMAP(ReponseBISAMAP.LOGIN_OK, null);
                            }
                            else
                                rep = new ReponseBISAMAP(ReponseBISAMAP.WRONG_LOGIN, null);
                        }
                        else
                            rep = new ReponseBISAMAP(ReponseBISAMAP.WRONG_LOGIN, null);
                    } catch (Exception ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.ALREADY_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.GET_NEXT_BILL) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Get_Next_Bill : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    cs.TraceEvenements(adresseDistante + "#Facture la plus ancienne non validée#" + Thread.currentThread().getName());
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.VALIDATE_BILL) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Validate_Bill : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String facture = req.getChargeUtile();

                if(loggedIn) {
                    cs.TraceEvenements(adresseDistante + "#Validation de " + facture + "#" + Thread.currentThread().getName());
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.LIST_BILLS) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de List_Bills : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String societe = parser[0];
                        String debut = parser[1];
                        String fin = parser[2];
                        cs.TraceEvenements(adresseDistante + "#Liste des factures de la société " + societe + " entre " + debut + " et " + fin + "#" + Thread.currentThread().getName());    
                        
                        
                    }
                    else
                        rep = new ReponseBISAMAP(ReponseBISAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.SEND_BILLS) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Send_Bills : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");
                    
                    Vector<String> factures = new Vector<>();
                    for(int i = 1; i < parser.length; i++)
                        factures.add(parser[i]);
                    cs.TraceEvenements(adresseDistante + "#Envoi de factures#" + Thread.currentThread().getName());
                    
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.REC_PAY) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Rec_Pay : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String facture = parser[0];
                        String montant = parser[1];
                        String infobanc = parser[2];
                        cs.TraceEvenements(adresseDistante + "#Enregistrement du paiement pour " + facture + "#" + Thread.currentThread().getName());

                        
                    }
                    else
                        rep = new ReponseBISAMAP(ReponseBISAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.LIST_WAITING) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de List_Waiting : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 1) {
                        String indic = parser[0];
                        cs.TraceEvenements(adresseDistante + "#Liste des factures non payées#" + Thread.currentThread().getName());

                        
                    }
                    else
                        rep = new ReponseBISAMAP(ReponseBISAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else
                rep = new ReponseBISAMAP(ReponseBISAMAP.UNKNOWN_TYPE, null);

            try {
                oos.writeObject(rep);
                oos.flush();
                
                req = (RequeteBISAMAP)ois.readObject();
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
            oos.writeObject(new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null));
            oos.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
}
