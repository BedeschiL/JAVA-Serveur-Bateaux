/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocolePLAMAP;

import java.io.Serializable;
import protocole.Reponse;

/**
 *
 * @author hector
 */
public class ReponsePLAMAP implements Reponse, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGIN_CONT_OK = 100;
    public static int WRONG_LOGIN = 101;
    public static int ALREADY_LOGGED_IN = 102;
    public static int GET_XY_OK = 200;
    public static int NO_SPACE_LEFT = 201;
    public static int SEND_WEIGHT_OK = 300;
    public static int GET_LIST_OK = 400;
    public static int SIGNAL_DEP_OK = 500;
    public static int SQL_ERROR = 501;
    public static int NOT_LOGGED_IN = 601;
    public static int INVALID_FORMAT = 888;
    public static int UNKNOWN_TYPE = 999;
    public static int SERVER_FAIL = -1;
    
    private int codeRetour;
    private String chargeUtile;
    
    public ReponsePLAMAP(int c, String chu) {
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
