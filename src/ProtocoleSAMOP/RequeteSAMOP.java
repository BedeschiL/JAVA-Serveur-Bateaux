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
package ProtocoleSAMOP;

//import beansForJdbc.BeanBDAccess;
import ProtocoleIOBREP.*;
import ProtocoleTRAMAP.*;
import ProtocoleCHAMAP.ReponseCHAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import RequeteSSL.RequeteSSL;
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
import javax.net.ssl.SSLSocket;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequeteSAMOP implements RequeteSSL, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;  
    public static int LOGIN = 1;
    public static int  COMPUTE_SAL = 2;
    public static int  VALIDATE_SAL = 3;
    public static int LAUNCH_PAYMENT  =4;
    public static int LAUNCH_PAYMENTS = 5;
    public static int ASK_PAYMENTS = 6;
      public static int CREATE_SAL = 7;
         public static int GET_SAL = 8;
   
    //FROM REPONSE   
    private int type;
    private String chargeUtile;
    private ObjectInputStream ois;  
    //Sauvegarde de l'ID du container SORTANT de HANDLER CONTAINER OUT
    private static String saveHandleContainer=null;
    
       //Sauvegarde de l'ID du container ENTNRANT de HANDLER CONTAINER IN
    private static String saveIncomingContainer=null;
     private static String saveEmplacement=null;
    public RequeteSAMOP(int t, String chu) {
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
    public Runnable createRunnable(final SSLSocket s, final ConsoleServeur cs) {      
        System.out.println("REQUETE SAMOP RUN");
        return new Runnable() {
            public void run() {
                try {
                     System.out.println("REQUETE SAMOP traiteRequeteLogin");
                    traiteRequeteLogin(s, cs);
                     System.out.println("REQUETE SAMOP traiteRequeteLogin-out");
                } catch (Exception ex) {
                    Logger.getLogger(RequeteSAMOP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };     
    }
    private void traiteRequeteLogin(SSLSocket sock, ConsoleServeur cs) throws Exception {
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
     
        boolean loggedIn = false;
        
        ObjectOutputStream oos = null;
        RequeteSAMOP req = this;
        ReponseSAMOP rep = null;
        while(true) {
            if(req.getType() == RequeteSAMOP.LOGIN && loggedIn == false) {
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
                            rs = db.executeRequeteSelection("SELECT *  FROM PERSONNEL WHERE password ='"+pass+"' AND login ='"+user+"'");
                              List allRows = new ArrayList();
                        List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs.next()){
                            String[] currentRow = new String[columnsNumber];
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs.getString(i));
                            }                      
                        }
                            //
                            
                            if(row.get(3).equals(user) && row.get(4).equals(pass))  {
                                System.out.println("equals");
                                loggedIn = true;
                                rep = new ReponseSAMOP(ReponseSAMOP.LOGIN_OK,"Vous etes un connecte en tant : " + row.get(6) + " : " + row.get(3) );
                            }
                            else
                                rep = new ReponseSAMOP(ReponseSAMOP.WRONG_LOGIN, "Mauvais login ou mot de passe");
                        } catch (Exception ex) {
                            rep = new ReponseSAMOP(ReponseSAMOP.SERVER_FAIL, "serveur fail");
                        }
                    }
                    else
                        rep = new ReponseSAMOP(ReponseSAMOP.INVALID_FORMAT, "format invalide");
                }
                else
                    rep = new ReponseSAMOP(ReponseSAMOP.ALREADY_LOGGED_IN, null);
               
            }
            //////////FIN DE LA GESTION DU LOGIN
            /////////DEBUT DE LA GESTION DE GET CONTAINER
            else if(req.getType() == RequeteSAMOP.COMPUTE_SAL)
            {
                
                ResultSet rs;
                System.out.println("RETOUR BD ");
                rs = db.executeRequeteSelection("SELECT *  FROM PERSONNEL");
                System.out.println("RETOUR BD ");
                List allRows = new ArrayList();
                        List<String> row= new ArrayList();
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnsNumber = rsmd.getColumnCount();
                        while(rs.next()){
                            String[] currentRow = new String[columnsNumber];
                            for(int i = 1;i<=columnsNumber;i++){
                                row.add(rs.getString(i));
                            }                      
                        }
                        int j=0;
                        Date date = new Date(); // This object contains the current date value
                        SimpleDateFormat formatter = new SimpleDateFormat("MM/yyyy");
                        String dat = formatter.format(date).toString();
                         int test =  row.size()/7;
                         System.out.println("test" + test);
                         
                         //TEMPORAIREMENT :
                         float salairebrut = 1000;
                         float fSalaireNet = 900;
                         //IL FAUDRA AJOUTER LES PRIMES EVENTUELLES
                        while(j<row.size())
                        {
                             System.out.println("Requete n°"+j);
                            db.executeRequeteMiseAJour("INSERT INTO SALAIRES VALUES(NULL,'"+row.get(j+1)+"','"+row.get(j+2)+"','"+dat+"',"+salairebrut+","+fSalaireNet+",NULL,0,0)");
                            j=j+7;                        
                        }
                        ResultSet rs2;
                            //RECUPERATION POUR AFFICHAGE DES SALAIRES NON ENVOIE/OU VALIDEE
                        rs2 =  db.executeRequeteSelection("SELECT * FROM SALAIRES WHERE ENVOIE=0");
                        List allRows2 = new ArrayList();
                        List<String> row2= new ArrayList();
                        ResultSetMetaData rsmd2 = rs2.getMetaData();
                        int columnsNumber2 = rsmd2.getColumnCount();
                        while(rs2.next()){
                            String[] currentRow2 = new String[columnsNumber2];
                            for(int i = 1;i<=columnsNumber2;i++){
                                row2.add(rs2.getString(i));
                            }                      
                        }
                        
                        
                        String arrayToString = null;
                        int i=0;
                        while(i<row2.size())
                        {    
                            if(i%columnsNumber2==0 )
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + " @ ";
                            }
                            arrayToString =arrayToString + row2.get(i);
                            i++;
                        }
                        System.out.println("arrayToString : " + arrayToString);
                            
                            
                         rep = new ReponseSAMOP(ReponseSAMOP.COMPUTE_SAL, arrayToString);   
            }
            else if(req.getType() == RequeteSAMOP.VALIDATE_SAL )
            {
              
                 
                
                
                
                
            }
            else if(req.getType() == RequeteSAMOP.LAUNCH_PAYMENT)
            {
                
               
              
            }
            else if(req.getType() == RequeteSAMOP.LAUNCH_PAYMENTS)
            {
                
            }   
            else if(req.getType() == RequeteSAMOP.ASK_PAYMENTS)
            {
               
            }
              else if(req.getType() == RequeteSAMOP.CREATE_SAL)
            {
                     String cu = req.getChargeUtile();
                     String[] parser = cu.split(":");               
                      if(parser.length >= 4) {
                        String nom = parser[0];
                        String prenom = parser[1];
                         String date = parser[2];
                         String salairebrut = parser[3];
                         float fSalaireBrut = Float.parseFloat(salairebrut);
                         float fSalaireNetPourc = (fSalaireBrut * 10)/ 100;
                         float fSalaireNet = fSalaireBrut - fSalaireNetPourc;
                         db.executeRequeteMiseAJour("INSERT INTO SALAIRES VALUES(NULL,'"+nom+"','"+prenom+"','"+date+"',"+salairebrut+","+fSalaireNet+",NULL,0,0)");
                         rep = new ReponseSAMOP(ReponseSAMOP.CREATE_SAL, "OK");
               
                    }
            }
              else if(req.getType() == RequeteSAMOP.GET_SAL)
              {
                   ResultSet rs2;
                            //RECUPERATION POUR AFFICHAGE DES SALAIRES NON ENVOIE/OU VALIDEE
                        rs2 =  db.executeRequeteSelection("SELECT * FROM SALAIRES WHERE ENVOIE=0");
                        List allRows2 = new ArrayList();
                        List<String> row2= new ArrayList();
                        ResultSetMetaData rsmd2 = rs2.getMetaData();
                        int columnsNumber2 = rsmd2.getColumnCount();
                        while(rs2.next()){
                            String[] currentRow2 = new String[columnsNumber2];
                            for(int i = 1;i<=columnsNumber2;i++){
                                row2.add(rs2.getString(i));
                            }                      
                        }
                        
                        
                        String arrayToString = null;
                        int i=0;
                        while(i<row2.size())
                        {    
                            if(i%columnsNumber2==0 )
                            {
                                arrayToString =arrayToString + " : ";
                            }
                            else
                            {
                                arrayToString =arrayToString + " @ ";
                            }
                            arrayToString =arrayToString + row2.get(i);
                            i++;
                        }
                        System.out.println("arrayToString : " + arrayToString);      
                         rep = new ReponseSAMOP(ReponseSAMOP.GET_SAL, arrayToString);   
                
                
                
                
                
                
              }
              if(oos==null)
                {
                     oos = new ObjectOutputStream(sock.getOutputStream());
                }
            oos.writeObject(rep);
                oos.flush();                
                req = (RequeteSAMOP)ois.readObject();
                
                
                
                         
                         
                         
                    
        /*    
        try {
            //sock.close();
           
            
        } catch (IOException e) {
            System.err.println("1Erreur ? [" + e.getMessage() + "]");
        }     */
        }
    }

   
}   

