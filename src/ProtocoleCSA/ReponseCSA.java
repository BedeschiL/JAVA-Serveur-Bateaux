/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocoleCSA;

import java.io.Serializable;
import protocole.Reponse;

/**
 *
 * @author hector
 */
public class ReponseCSA implements Reponse, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGINA_OK = 100;
    public static int WRONG_LOGIN = 101;
    public static int ALREADY_LOGGED_IN = 102;
    public static int NOT_LOGGED_IN = 601;
    public static int INVALID_FORMAT = 888;
    public static int UNKNOWN_TYPE = 999;
    public static int SERVER_FAIL = -1;
    
    private int codeRetour;
    private String chargeUtile;
    
    public ReponseCSA(int c, String chu) {
        codeRetour = c;
        setChargeUtile(chu);
    }
    
    @Override
    public int getCode() {
        return codeRetour;
    }
    
    public String getChargeUtile() {
        return chargeUtile;
    }

    public void setChargeUtile(String chargeUtile) {
        this.chargeUtile = chargeUtile;
    }
}
