package net.hairi.Thales;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: acer
 * Date: 14 Jul 11
 * Time: 11:35:25
 * To change this template use File | Settings | File Templates.
 */
public class HThalesKeyScheme {


    public static char U = 'U';
    public static char Z = 'Z';
    public static char X = 'X';
    public static char Y = 'Y';
    public static char T = 'T';


    static Map keySchemes = new LinkedHashMap();

    
    static {
        keySchemes.put(Z, 16);
        keySchemes.put(U, 32);
        keySchemes.put(X, 32);
        keySchemes.put(Y, 48);
        keySchemes.put(T, 48);






    
    }


     public  static int getKeyLength(char b) throws Exception
     {


     int     ret =   ((Integer) keySchemes.get(b)).intValue();


        return ret;








     }



    


    



}
