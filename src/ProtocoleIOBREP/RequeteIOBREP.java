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
package ProtocoleIOBREP;

//import beansForJdbc.BeanBDAccess;
import ProtocoleTRAMAP.*;
import ProtocoleCHAMAP.ReponseCHAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import beansForJdbc.BeanBDAccess;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.*;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequeteIOBREP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;  
    public static int LOGIN = 1;
    public static int  GET_CONTAINERS = 2;
    public static int  HANDLE_CONTAINER_OUT = 3;
    public static int END_CONTAINER_OUT =4;
    public static int BOAT_ARRIVED = 5;
    public static int HANDLE_CONTAINER_IN = 6;
    public static int END_CONTAINER_IN  = 7;
     public static int GET_NEWS = 8;
     public static int GET_STATE = 9;
    //FROM REPONSE   
    private int type;
    private String chargeUtile;
    private ObjectInputStream ois;  
    //Sauvegarde de l'ID du container SORTANT de HANDLER CONTAINER OUT
    private static String saveHandleContainer=null;
    
       //Sauvegarde de l'ID du container ENTNRANT de HANDLER CONTAINER IN
    private static String saveIncomingContainer=null;
     private static String saveEmplacement=null;
    public RequeteIOBREP(int t, String chu) {
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
        System.out.println("REQUETE IOBREP RUN0");
        return new Runnable() {
            public void run() {
                try {
                     System.out.println("REQUETE IOBREP traiteRequeteLogin");
                    traiteRequeteLogin(s, cs);
                     System.out.println("REQUETE IOBREP traiteRequeteLogin-out");
                } catch (Exception ex) {
                    Logger.getLogger(RequeteIOBREP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };     
    }
    private void traiteRequeteLogin(Socket sock, ConsoleServeur cs) throws Exception {
        System.out.println("REQUETE DBACESS");
        BeanBDAccess db = new BeanBDAccess("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@localhost:1521:xe", "louis", "gpt");
        try {
            db.creerConnexionBD();
        }
        catch (Exception ex) {
            System.out.println("REQUETE DBACESS FAIL");
            System.out.println(ex.getMessage());
            return;
        }
        BeanBDAccess db2 = new BeanBDAccess("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@localhost:1521:xe", "parc", "Gpt");
        try {
            db2.creerConnexionBD();
        }
        catch (Exception ex) {
            System.out.println("REQUETE DBACESS FAIL");
            System.out.println(ex.getMessage());
            return;
        }
        System.out.println("REQUETE DBACESS2");
        boolean loggedIn = false;
        
        ObjectOutputStream oos = null;
        RequeteIOBREP req = this;
        ReponseIOBREP rep = null;
        while(true) {
            if(req.getType() == RequeteIOBREP.LOGIN && loggedIn == false) {
                System.out.println("REQUETE LOGIN");
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();
                System.out.println("Début de Login :  charge utiles = " +  req.getChargeUtile());
                if(!loggedIn) {
                    String[] parser = cu.split(":");               
                    if(parser.length >= 2) {
                        String user = parser[0];
                        String pass = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Connexion de " + user + "; MDP = " + pass + "#" + Thread.currentThread().getName());
                        ResultSet rs;
                        try {
                            System.out.println("RETOUR BD ");
                            rs = db.executeRequeteSelection("SELECT COUNT(*) as valeur  FROM PERSONNEL WHERE password ='"+pass+"' AND login ='"+user+"'");
                            System.out.println("RETOUR BD ");
                            String columnValue=null;
                            ResultSetMetaData rsmd = rs.getMetaData();
                            int columnsNumber = rsmd.getColumnCount();
                            while (rs.next()) {
                                for (int i = 1; i <= columnsNumber; i++) {
                                    if (i > 1) System.out.print(",  ");
                                    columnValue = rs.getString(i);
                                }
                            }
                            System.out.println(columnValue);
                            //
                            
                            if(columnValue.equals("1")) {
                                System.out.println("equals");
                                loggedIn = true;
                                rep = new ReponseIOBREP(ReponseIOBREP.LOGIN_OK, "Votre mot de passe et login sont bons !");
                            }
                            else
                                rep = new ReponseIOBREP(ReponseIOBREP.WRONG_LOGIN, "Mauvais login ou mot de passe");
                        } catch (Exception ex) {
                            rep = new ReponseIOBREP(ReponseIOBREP.SERVER_FAIL, "serveur fail");
                        }
                    }
                    else
                        rep = new ReponseIOBREP(ReponseIOBREP.INVALID_FORMAT, "format invalide");
                }
                else
                    rep = new ReponseIOBREP(ReponseIOBREP.ALREADY_LOGGED_IN, null);
            }
            //////////FIN DE LA GESTION DU LOGIN
            /////////DEBUT DE LA GESTION DE GET CONTAINER
            else if(req.getType() == RequeteIOBREP.GET_CONTAINERS)
            {
                System.out.println("--------------------------");
                System.out.println("GET_CONTAINERS"); 
                try
                {
                    String destOrder = req.getChargeUtile();
                    String dest=null;
                    String order=null;
                    String[] parser = destOrder.split(":");
                    if(parser.length >= 2) {
                        dest = parser[0];
                        order = parser[1];
                    }
                    if(order !=null && dest !=null)
                    {
                        System.out.println("dest" + dest);
                        System.out.println("order" + order);
                        ResultSet rs2;
                        if(order.toUpperCase().equals("FIRST"))
                        {
                            rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE DESTINATION LIKE '"+dest+"' ORDER BY ARRIVAL");
                        }
                        else
                        {
                            rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE DESTINATION LIKE '"+dest+"'");
                        }                 
                        System.out.println("Apres rs2");
                        List allRows = new ArrayList();
                        List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs2.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs2.next()){
                            String[] currentRow = new String[columnsNumber];
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs2.getString(i));
                            }                      
                        }
                        System.out.println("Apres 0");
                        int i = 0;
                        String arrayToString=null;
                        System.out.println("Apres 1");
                        while(i<row.size())
                        {    
                            if(i%columnsNumber==0)
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + " @ ";
                            }
                            arrayToString =arrayToString + row.get(i);
                            i++;
                        }
                        
                        System.out.println("arrayToString");
                        System.out.println("arrayToString  " + arrayToString);
                        System.out.println("Apres 2");
                        rep = new ReponseIOBREP(ReponseIOBREP.GET_CONTAINER,arrayToString );
                        
                        System.out.println("Apres 3");
                    }
                    else
                    {
                        rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"Pas de valeur pour dest ou order" );
                    }
                    
                } catch (Exception ex) {
                    Logger.getLogger(RequeteIOBREP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if(req.getType() == RequeteIOBREP.HANDLE_CONTAINER_OUT )
            {
                System.out.println("--------------------------");
                System.out.println("HANDLE_CONTAINER_OUT");
                try
                {
                    String contOrder = req.getChargeUtile();
                    String cont=null;
                    String order=null;
                    String[] parser = contOrder.split(":");
                    if(parser.length >= 2) {
                        cont = parser[0];
                        order = parser[1];
                    }
                    System.out.println("cont" + cont);
                    System.out.println("order" + order);
                    ResultSet rs2;
                    if(order !=null && order.toUpperCase().equals("FIRST"))
                    {
                         System.out.println("FIRST");
                        rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE IDENTIFIANT LIKE '"+cont+"' AND ARRIVAL = (SELECT MIN(ARRIVAL) FROM PARC)");
                    }
                    else
                    {
                        System.out.println("RANDOM");
                        rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE IDENTIFIANT LIKE '"+cont+"'");
                    }
                        List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs2.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs2.next()){
                            String[] currentRow = new String[columnsNumber];
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs2.getString(i));
                            }                      
                        }
                        System.out.println("Apres 0");
                        int i = 0;
                        String arrayToString=null;
                        System.out.println("Apres 1");
                        while(i<row.size())
                        {    
                            if(i%columnsNumber==0)
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + " @ ";
                            }
                            arrayToString =arrayToString + row.get(i);
                            i++;
                        }
                        System.out.println("arrayToString");
                        System.out.println("arrayToString  " + arrayToString);
                        if(arrayToString !=null)
                        {
                           System.out.println("Apres 2");
                          rep = new ReponseIOBREP(ReponseIOBREP.HANDLE_CONTAINER_OUT,arrayToString + " @ le container  ù"+cont+"ù va etre deplace");
                          saveHandleContainer = cont;
                           System.out.println("Apres 3");
                        }
                        else
                        {
                             rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"Aucune data trouver dans la BD car le container le plus vieux n'est pas le bon" );
                        }     
                } catch (Exception ex) {
                    Logger.getLogger(RequeteIOBREP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if(req.getType() == RequeteIOBREP.END_CONTAINER_OUT)
            {
                  System.out.println("END_CONTAINER_OUT");
                    System.out.println("saveHandleContainer " + saveHandleContainer);
                if(saveHandleContainer!=null)
                {
                    System.out.println("--------------------------");
                    ResultSet rs2;
                    rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE IDENTIFIANT like'"+saveHandleContainer+"'");
                       List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs2.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs2.next()){
                            String[] currentRow = new String[columnsNumber];
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs2.getString(i));
                            }                      
                        }
                        System.out.println("END_CONTAINER_OUTApres  0");
                        int i = 0;
                        String arrayToString=null;
                        System.out.println("END_CONTAINER_OUT Apres 1");
                        while(i<row.size())
                        {    
                            if(i%columnsNumber==0)
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + "@";
                            }
                            arrayToString =arrayToString + row.get(i);
                            i++;
                        }              
                        System.out.println("END_CONTAINER_OUT arrayToString  " + arrayToString);
                         String[] parserNull = arrayToString.split(":");
                        
                            String[] parserParc =null;       
                         if(parserNull.length >1)
                         {
                          parserParc  = parserNull[1].split("@");
                      
                        }
                        int y=0;
                        while(y< parserParc.length)
                        {
                                System.out.println("END_CONTAINER_OUT parc tab 00 " + parserParc[y]);
                                y++;
                        }
                        //A FAIRE NOM DU BATEAU MANQUANT
                        Date date = new Date(); // This object contains the current date value
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/M/yyyy HH-mm-ss");
                        String dat = formatter.format(date).toString();
                    String insert="INSERT INTO MOUVEMENTS VALUES('"+parserParc[1]+dat+"E_C_Out"+"','"+parserParc[1]+"',NULL,NULL,NULL,NULL,0,'"+dat+"',NULL)";
                    System.out.println(insert);
                     db2.executeRequeteMiseAJour(insert);
                     //MISE A JOUR DE MOUVEMENT + TRANSPORTEUR
                     
                     //MISE A JOUR DU PARC SUPPRESION DES DONNEES DE L'EMPLACEMENT
                     //&&& A FAIRE &&&& //METTRE PLUS D'INFO DEPUIS ANDROID ! 
                     db2.executeRequeteMiseAJour("UPDATE PARC SET IDENTIFIANT = null, ETAT = 1, RESERVATION = null, ARRIVAL = null, POIDS = null, DESTINATION = null,TRANSPORT =null  WHERE IDENTIFIANT like'"+saveHandleContainer+"'");   
                   
                     rep = new ReponseIOBREP(ReponseIOBREP.END_CONTAINER_OUT,"Update confirmation de la mise a jours du parc et mouvement ok" );
                     saveHandleContainer=null;
                }
                else
                {
                    rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"Update non faite, container non trouve ! Pas de maj" );
                    saveHandleContainer=null;
                } 
            }
            else if(req.getType() == RequeteIOBREP.BOAT_ARRIVED)
            {
                System.out.println("--------------------------");
                System.out.println("HANDLE_CONTAINER_OUT");
                String idBatDest = req.getChargeUtile();
                 System.out.println("HANDLE_CONTAINER_OUT idBatDest " + idBatDest);
                String idBat=null;
                String idSociete=null;
                String capacite =null;
                String carac =null;
                String idCont=null;
                String poids=null;
                String dest=null;
                String[] parser = idBatDest.split(":");
                    if(parser.length >= 5) {
                        idBat = parser[0];
                        idSociete = parser[1];
                        capacite = parser[2];
                        
                        poids = parser[3];
                        idCont = parser[4];
                        dest = parser[5];
                        
                    }
                if(idBat !=null && idSociete !=null)
                {
                    try
                    {
                           db2.executeRequeteMiseAJour("INSERT INTO TRANSPORTEUR VALUES('"+idBat+"','"+idSociete+"',"+capacite+",'Bateau')");
                    }
                    catch(SQLIntegrityConstraintViolationException e)
                    {
                                  System.out.println("Transporteur Deja contenu");
                    }
                     Date date = new Date(); // This object contains the current date value
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/M/yyyy HH-mm-ss");
                        String dat = formatter.format(date).toString();
                     db2.executeRequeteMiseAJour("INSERT INTO MOUVEMENTS VALUES('"+idBat+idCont+dat+"B-A"+"','"+idCont+"','"+idSociete+"',NULL,NULL,'"+dat+"',"+poids+",NULL,'"+dest+"')");
                      rep = new ReponseIOBREP(ReponseIOBREP.BOAT_ARRIVED,"Insert dans transporteur ok et mouvement " );
                }
                else
                {   
                 rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"Une erreur dans l'arrive du bateau s'est produite" );
                }
            }   
            else if(req.getType() == RequeteIOBREP.HANDLE_CONTAINER_IN)
            {
                System.out.println("--------------------------");
                   String idCont = req.getChargeUtile();
                   System.out.println("CONTAINER IN : ID " + idCont);
                   //A FINIR RECUPERER PLUS D'INFO DU CONTENEUR
                   saveIncomingContainer = idCont;
                   ResultSet rs2;
                   rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE ROWNUM = 1 AND ETAT =1");
                     List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs2.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs2.next()){
                          
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs2.getString(i));
                            }                      
                        }
                        System.out.println("HANDLE_CONTAINER_IN  0");
                        int i = 0;
                        String arrayToString=null;
                        System.out.println("HANDLE_CONTAINER_IN  1");
                        while(i<row.size())
                        {    
                            if(i%columnsNumber==0)
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + "@";
                            }
                            arrayToString =arrayToString + row.get(i);
                            i++;
                        }
                          if(arrayToString==null)
                          {
                                 rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"There is no free places to handle your container" );
                          }
                          else
                          {
                               rep = new ReponseIOBREP(ReponseIOBREP.HANDLE_CONTAINER_IN,"There is a free place ! at " + row.get(0));
                               saveEmplacement = row.get(0);
                                 System.out.println("HANDLE_CONTAINER_IN fin : free place at "+ row.get(0) + " et cont : " + saveIncomingContainer);
                          }
                
            }
            else if(req.getType() == RequeteIOBREP.END_CONTAINER_IN)
            {
                 System.out.println("--------------------------");
                        System.out.println("END_CONTAINER_IN cont " + saveIncomingContainer +" save emp " + saveEmplacement);
                       
                    if(saveIncomingContainer!=null)
                    {
                          Date date = new Date(); // This object contains the current date value
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/M/yyyy HH-mm-ss");
                        String dat = formatter.format(date).toString();
                         System.out.println("END CONTAINER_IN DATE AVEC TIRET " + dat);
                        // A FINIR GET LES INFOS DU CONTAINEUR
                          db2.executeRequeteMiseAJour("INSERT INTO MOUVEMENTS VALUES('"+saveIncomingContainer+dat+"E_C_IN"+"','"+saveIncomingContainer+"',NULL,NULL,NULL,'"+dat+"',NULL,NULL,NULL)");
                         // A FAIRE : db2.executeRequeteMiseAJour("INSERT INTO TRANSPORTEUR VALUES('"+idBat+"','"+idSociete+"',"+capacite+",'"+carac+"')");
                         db2.executeRequeteMiseAJour("UPDATE PARC SET IDENTIFIANT = '"+saveIncomingContainer+"', ETAT = 2, RESERVATION = null, ARRIVAL = '"+dat+"', POIDS = null, DESTINATION = null,TRANSPORT =null  WHERE COORDONNEE like'"+saveEmplacement+"'");   
                        
                            System.out.println("Container valide");
                         rep = new ReponseIOBREP(ReponseIOBREP.END_CONTAINER_IN,"Valide pour emp " + saveEmplacement + " Containeur : "+saveIncomingContainer);
                    }
                    else
                    {
                           rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"pas de containeur recu ! Il en faut un pour valider" );
                    }
            }
             else if(req.getType() == RequeteIOBREP.GET_NEWS)
             {
                 
             }
             else if(req.getType() == RequeteIOBREP.GET_STATE)
             {
                   try
                {              
                        ResultSet rs2;
                        rs2 = db2.executeRequeteSelection("SELECT * FROM PARC WHERE ROWNUM <3 ORDER BY ARRIVAL;");           
                        System.out.println("Apres rs2");
                        List allRows = new ArrayList();
                        List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs2.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs2.next()){
                            String[] currentRow = new String[columnsNumber];
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs2.getString(i));
                            }                      
                        }
                        System.out.println("Apres 0");
                        int i = 0;
                        String arrayToString=null;
                        System.out.println("Apres 1");
                        while(i<row.size())
                        {    
                            if(i%columnsNumber==0)
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + " @ ";
                            }
                            arrayToString =arrayToString + row.get(i);
                            i++;
                        }
                        if(arrayToString==null)
                        {
                               rep = new ReponseIOBREP(ReponseIOBREP.UNKNOWN_TYPE,"Pas de containeur a visualise" );     
                        }
                        else
                        {
                            System.out.println("arrayToString");
                            System.out.println("arrayToString  " + arrayToString);
                            System.out.println("Apres 2");
                            rep = new ReponseIOBREP(ReponseIOBREP.GET_CONTAINER,arrayToString );      
                            System.out.println("Apres 3");
                    
                        }
                } catch (Exception ex) {
                    Logger.getLogger(RequeteIOBREP.class.getName()).log(Level.SEVERE, null, ex);
                }
             }
            try {
                if(oos==null)
                {
                     oos = new ObjectOutputStream(sock.getOutputStream());
                }
             
                oos.writeObject(rep);
                oos.flush();                
                req = (RequeteIOBREP)ois.readObject();
              
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
                
            }
            catch(Exception e) {
          
                System.err.println("0Erreur ? [" + e.getMessage() + "]");
                break;
            }
        }      
        try {
            sock.close();
           
            
        } catch (IOException e) {
            System.err.println("1Erreur ? [" + e.getMessage() + "]");
        }     
    }   
}
