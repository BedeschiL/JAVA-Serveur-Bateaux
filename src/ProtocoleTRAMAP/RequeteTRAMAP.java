/*
 * Copyright (C) 2020 hector
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ProtocoleTRAMAP;

//import beansForJdbc.BeanBDAccess;
import ProtocoleCHAMAP.ReponseCHAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import beansForJdbc.BeanBDAccess;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import protocole.Requete;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.*;
import java.sql.ResultSet;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocole.ConsoleServeur;

/**
 *
 * @author hector
 */
public class RequeteTRAMAP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static String codeProvider = "BC"; //CryptixCrypto";
    
    public static int LOGIN = 1;
    public static int INPUT_LORRY = 2;
    public static int INPUT_LORRY_WITHOUT_RESERVATION = 3;
    public static int OUTPUT_CONTAINER = 4;
    public static int LIST_OPERATIONS = 5;
    public static int LOGOUT = 6;
    public static int ADD_TO_DB = 7;
    
    private int type;
    private String chargeUtile;
    private ObjectInputStream ois;

    public RequeteTRAMAP(int t, String chu) {
        type = t;
        chargeUtile = chu;
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
    public Runnable createRunnable(final Socket s, final ConsoleServeur cs) {
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
        RequeteTRAMAP req = this;
        ReponseTRAMAP rep = null;
        
        while(true) {
            if(req.getType() == RequeteTRAMAP.LOGIN) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

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
                                rep = new ReponseTRAMAP(ReponseTRAMAP.LOGIN_OK, null);
                            }
                            else
                                rep = new ReponseTRAMAP(ReponseTRAMAP.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.ALREADY_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteTRAMAP.INPUT_LORRY) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début d'Input_Lorry : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 10) {
                        String reservation = parser[0];
                        String container = parser[1];
                        String transEntrant = parser[2]; 
                        String dateArrivee = parser[3];
                        String poids = parser[4];
                        String destination = parser[5];
                        String contenu = parser[6];
                        String capacite = parser[7];
                        String dangers = parser[8];
                        String societe = parser[9];
                        cs.TraceEvenements(adresseDistante + "#Arrivée de " + container + " avec réservation " + reservation + "#" + Thread.currentThread().getName());

                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM occupations WHERE dateDebut <= CAST('" + dateArrivee + "' AS DATE) AND dateFin >= CAST('" + dateArrivee + "' AS DATE) AND id = " + reservation);
                            if(!rs.next())
                                rep = new ReponseTRAMAP(ReponseTRAMAP.RESERVATION_NOT_FOUND, null);
                            else {
                                String x = rs.getString("x");
                                String y = rs.getString("y");
                                try {
                                    db.executeRequeteMiseAJour("INSERT INTO containers VALUES (" + container + ", '" + societe + "', '" + contenu + "', " + capacite + ", '" + dangers + "')");
                                } catch (Exception e) {
                                    try {
                                        db.executeRequeteMiseAJour("UPDATE containers SET proprietaire = '" + societe + "', contenu = '" + contenu + "', capacite = " + capacite + ", dangers = '" + dangers + "' WHERE id = " + container);
                                    } catch (Exception ex) {
                                        System.err.println("Erreur ? [" + ex.getMessage() + "]");
                                        rep = new ReponseTRAMAP(ReponseTRAMAP.SQL_ERROR, ex.getMessage());
                                    }
                                }
                                rs = db.executeRequeteSelection("SELECT * FROM mouvements WHERE dateDepart IS NULL AND container = " + container);
                                if(rs.next())
                                    rep = new ReponseTRAMAP(ReponseTRAMAP.CONTAINER_ALREADY_PRESENT, null);
                                else {
                                    try {
                                        db.executeRequeteMiseAJour("INSERT INTO mouvements (container, transEntrant, dateArrivee, poids, destination) VALUES (" + container + ", '" + transEntrant + "', CAST('" + dateArrivee + "' AS DATE), " + poids + ", " + destination + ")");
                                        rep = new ReponseTRAMAP(ReponseTRAMAP.INPUT_LORRY_OK, x + ", " + y);
                                    } catch (Exception ex) {
                                        System.err.println("Erreur ? [" + ex.getMessage() + "]");
                                        rep = new ReponseTRAMAP(ReponseTRAMAP.SQL_ERROR, ex.getMessage());
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Erreur ? [" + ex.getMessage() + "]");
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteTRAMAP.INPUT_LORRY_WITHOUT_RESERVATION) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début d'Input_Lorry_Without_Reservation : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 9) {
                        String container = parser[0];
                        String transEntrant = parser[1]; 
                        String dateArrivee = parser[2];
                        String poids = parser[3];
                        String destination = parser[4];
                        String contenu = parser[5];
                        String capacite = parser[6];
                        String dangers = parser[7];
                        String societe = parser[8];
                        cs.TraceEvenements(adresseDistante + "#Arrivée de " + container + " sans réservation#" + Thread.currentThread().getName());
                        try {
                            db.executeRequeteMiseAJour("INSERT INTO containers VALUES (" + container + ", '" + societe + "', '" + contenu + "', " + capacite + ", '" + dangers + "')");
                        } catch (Exception e) {
                            try {
                                db.executeRequeteMiseAJour("UPDATE containers SET proprietaire = '" + societe + "', contenu = '" + contenu + "', capacite = " + capacite + ", dangers = '" + dangers + "' WHERE id = " + container);
                            } catch (Exception ex) {
                                System.err.println("Erreur ? [" + ex.getMessage() + "]");
                                rep = new ReponseTRAMAP(ReponseTRAMAP.SQL_ERROR, ex.getMessage());
                            }
                        }
                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM mouvements WHERE dateDepart IS NULL AND container = " + container);
                            if(rs.next())
                                rep = new ReponseTRAMAP(ReponseTRAMAP.CONTAINER_ALREADY_PRESENT, null);
                            else {
                                try {
                                    
                                    db.executeRequeteMiseAJour("INSERT INTO mouvements (container, transEntrant, dateArrivee, poids, destination) VALUES (" + container + ", '" + transEntrant + "', CAST('" + dateArrivee + "' AS DATE), " + poids + ", " + destination + ")");
                                    rep = new ReponseTRAMAP(ReponseTRAMAP.INPUT_LORRY_WITHOUT_RESERVATION_OK, "1, 1");
                                } catch (Exception ex) {
                                    System.err.println("Erreur ? [" + ex.getMessage() + "]");
                                    rep = new ReponseTRAMAP(ReponseTRAMAP.SQL_ERROR, ex.getMessage());
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Erreur ? [" + ex.getMessage() + "]");
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteTRAMAP.OUTPUT_CONTAINER) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début d'Output_Container : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String container = parser[0];
                        String transSortant = parser[1]; 
                        String dateDepart = parser[2];
                        cs.TraceEvenements(adresseDistante + "#Départ de " + container + "#" + Thread.currentThread().getName());
                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM mouvements WHERE dateDepart IS NULL AND container = " + container);
                            if(!rs.next())
                                rep = new ReponseTRAMAP(ReponseTRAMAP.CONTAINER_NOT_FOUND, null);
                            else {
                                db.executeRequeteMiseAJour("UPDATE mouvements SET transSortant = '" + transSortant + "', dateDepart = CAST('" + dateDepart + "' AS DATE) WHERE dateDepart IS NULL AND container = " + container);
                                rep = new ReponseTRAMAP(ReponseTRAMAP.OUTPUT_CONTAINER_OK, null);
                            }
                        } catch (Exception ex) {
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SQL_ERROR, ex.getMessage());
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteTRAMAP.LIST_OPERATIONS) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de List_Operations : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 4) {
                        String type = parser[0];
                        String debutIntervalle = parser[1]; 
                        String finIntervalle = parser[2];
                        String critere = parser[3];
                        cs.TraceEvenements(adresseDistante + "#Liste des mouvements entre " + debutIntervalle + " et " + finIntervalle + " concernant " + critere + "#" + Thread.currentThread().getName());
                        ResultSet rs;
                        try {
                            if(type.equals("D"))
                                rs = db.executeRequeteSelection("SELECT * FROM mouvements WHERE dateArrivee >= CAST('" + debutIntervalle + "' AS DATE) AND dateDepart <= CAST('" + finIntervalle + "' AS DATE) AND destination = " + critere);
                            else {
                                rs = db.executeRequeteSelection("SELECT * FROM mouvements WHERE dateArrivee >= CAST('" + debutIntervalle + "' AS DATE) AND dateDepart <= CAST('" + finIntervalle + "' AS DATE) AND (SELECT proprietaire FROM containers WHERE id = container LIMIT 1) = '" + critere + "'");
                            }

                            String chargeUtile = "";

                            while(rs.next()) {
                                chargeUtile += rs.getString("id");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("container");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("transEntrant");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("dateArrivee");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("transSortant");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("dateDepart");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("poids");
                                chargeUtile += "  ";
                                chargeUtile += rs.getString("destination");
                                chargeUtile += "  ";
                            }
                            rep = new ReponseTRAMAP(ReponseTRAMAP.LIST_OPERATIONS_OK, chargeUtile);
                        } catch (Exception ex) {
                            System.err.println("Erreur ? [" + ex.getMessage() + "]");
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteTRAMAP.LOGOUT) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Logout : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String user = parser[0];
                        String pass = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Déconnexion de " + user + "; MDP = " + pass + "#" + Thread.currentThread().getName());
                        ResultSet rs;
                        try {
                            rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + user + "'");
                            if(rs.next() && pass.equals(rs.getString("pass")))
                            {
                                loggedIn = false;
                                rep = new ReponseTRAMAP(ReponseTRAMAP.LOGOUT_OK, null);
                            }
                            else
                                rep = new ReponseTRAMAP(ReponseTRAMAP.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null);
            }
            else if(req.getType() == RequeteTRAMAP.ADD_TO_DB) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de List_Operations : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 1) {
                        String type = parser[0];
                        cs.TraceEvenements(adresseDistante + "#Ajout dans la base de données d'un tuple#" + Thread.currentThread().getName());
                        ResultSet rs;
                        try {
                            if(type.equals("S")) {
                                String idS = null, nomS = null, emailS = null, telephoneS = null, adresseS = null;
                                if(parser.length >= 6) {
                                    idS = parser[1];
                                    nomS = parser[2];
                                    emailS = parser[3];
                                    telephoneS = parser[4];
                                    adresseS = parser[5];
                                }
                                db.executeRequeteMiseAJour("INSERT INTO societes VALUES ('" + idS + "', '" + nomS + "', '" + emailS + "', '" + telephoneS + "', '" + adresseS + "')");
                            }
                            else if(type.equals("T")) {
                                String idT = null, proprietaireT = null, capaciteT = null, caracTechT = null;
                                if(parser.length >= 5) {
                                    idT = parser[1];
                                    proprietaireT = parser[2];
                                    capaciteT = parser[3];
                                    caracTechT = parser[4];
                                }
                                db.executeRequeteMiseAJour("INSERT INTO transporteurs VALUES ('" + idT + "', '" + proprietaireT + "', " + capaciteT + ", '" + caracTechT + "')");
                            }
                            else {
                                String idD = null, villeD = null, bateauD = null, trainD = null, routeD = null;
                                if(parser.length >= 6) {
                                    idD = parser[1];
                                    villeD = parser[2];
                                    bateauD = parser[3];
                                    trainD = parser[4];
                                    routeD = parser[5];
                                }
                                db.executeRequeteMiseAJour("INSERT INTO destinations VALUES (" + idD + ", '" + villeD + "', " + bateauD + ", " + trainD + ", " + routeD + ")");
                            }
                            rep = new ReponseTRAMAP(ReponseTRAMAP.ADD_TO_DB_OK, null);
                        } catch (Exception ex) {
                            System.err.println("Erreur ? [" + ex.getMessage() + "]");
                            rep = new ReponseTRAMAP(ReponseTRAMAP.SQL_ERROR, ex.getMessage());
                        }
                    }
                    else
                        rep = new ReponseTRAMAP(ReponseTRAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null);
            }
            else
                rep = new ReponseTRAMAP(ReponseTRAMAP.UNKNOWN_TYPE, null);

            try {
                oos.writeObject(rep);
                oos.flush();
                
                req = (RequeteTRAMAP)ois.readObject();
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
            }
            catch(Exception e) {
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
            oos.writeObject(new ReponseTRAMAP(ReponseTRAMAP.NOT_LOGGED_IN, null));
            oos.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
}
