/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocoleCHAMAP;

import beansForJdbc.BeanBDAccess;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.*;
import java.sql.ResultSet;
import java.util.Vector;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequeteCHAMAP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static String codeProvider = "BC"; //CryptixCrypto";
    
    public static int LOGIN_TRAF = 1;
    public static int MAKE_BILL = 2;
    
    private int type;
    private String chargeUtile;
    private byte[] digest;
    private Socket socketClient;
    private ObjectInputStream ois;

    public RequeteCHAMAP(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }
    
    public RequeteCHAMAP(int t, String chu, byte[] dig) {
        type = t;
        chargeUtile = chu;
        digest = dig;
    }
    
    public RequeteCHAMAP(int t, String chu, Socket s) {
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
        if(type == LOGIN_TRAF) {
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
        RequeteCHAMAP req = null;
        ReponseCHAMAP rep = null;
        
        try {
            oos = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException ex) {
            System.err.println("Erreur ? [" + ex.getMessage() + "]");
        }
        
        while(true) {
            if(req.getType() == RequeteCHAMAP.LOGIN_TRAF) {
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login_Traf : adresse distante = " + adresseDistante);
                // la charge utile est le nom
                String cu = req.getChargeUtile();
                
                if(!loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String user = parser[0];
                        String temps = parser[1];
                        String alea = parser[2];
                        
                        cs.TraceEvenements(adresseDistante + "#Connexion de " + user + "#" + Thread.currentThread().getName());
                    
                        ResultSet rs;
                        try {
                            rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + user + "'");
                            if(rs.next())
                            {
                                String pass = rs.getString("pass");

                                // confection d'un digest local
                                MessageDigest md = MessageDigest.getInstance("SHA-1", codeProvider);
                                md.update(user.getBytes());
                                md.update(pass.getBytes());
                                
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                DataOutputStream bdos = new DataOutputStream(baos);
                                bdos.writeLong(Long.parseLong(temps));
                                bdos.writeDouble(Double.parseDouble(alea));
                                
                                md.update(baos.toByteArray());
                                byte[] msgDLocal = md.digest();

                                if(msgDLocal.equals(req.getDigest())) {
                                    loggedIn = true;
                                    rep = new ReponseCHAMAP(ReponseCHAMAP.LOGIN_TRAF_OK, null);
                                }
                                else
                                    rep = new ReponseCHAMAP(ReponseCHAMAP.WRONG_LOGIN, null);
                            }
                            else
                                rep = new ReponseCHAMAP(ReponseCHAMAP.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponseCHAMAP(ReponseCHAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseCHAMAP(ReponseCHAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseCHAMAP(ReponseCHAMAP.ALREADY_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteCHAMAP.MAKE_BILL) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Make_Bill : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String identifiant = parser[0];
                        Vector<String> containers = new Vector<>();
                        for(int i = 1; i < parser.length; i++)
                            containers.add(parser[i]);
                        cs.TraceEvenements(adresseDistante + "#Génération des factures pour " + identifiant + "#" + Thread.currentThread().getName());

                        
                    }
                    else
                        rep = new ReponseCHAMAP(ReponseCHAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseCHAMAP(ReponseCHAMAP.NOT_LOGGED_IN, null);
            }
            else
                rep = new ReponseCHAMAP(ReponseCHAMAP.UNKNOWN_TYPE, null);

            try {
                oos.writeObject(rep);
                oos.flush();
                
                req = (RequeteCHAMAP)ois.readObject();
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
            oos.writeObject(new ReponseCHAMAP(ReponseCHAMAP.NOT_LOGGED_IN, null));
            oos.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
    
}
