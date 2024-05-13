/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author NJINU
 */
public class DecryptValueMOB
{

    public static FLWorkerMOB fLWorker = new FLWorkerMOB();

//    public static void main(String[] args)
//    {
//        try
//        {
//            decryptdata();
//        }
//        catch (IOException ex)
//        {
//            Logger.getLogger(DecryptValueMOB.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

    public static void decryptdata() throws FileNotFoundException, IOException
    {
        FileInputStream in;
        FileOutputStream ou;
        // AccountBalance250616035600.csv
        //ACCOUNTBALANCES.TXT
        in = new FileInputStream("D:\\Philae\\files\\ACCOUNTBALANCES.csv");
        // in = new FileInputStream("D:\\Philae\\files\\AccountBalance250616035600.csv");
        //FLWorker fLWorker =  new FLWorkerMOB();

        BufferedReader rd = new BufferedReader(new InputStreamReader(in));
        String line;

        while ((line = rd.readLine()) != null)
        {
            System.out.println(line);
            System.err.println(fLWorker.decrypt(line));

        }
    }
}
