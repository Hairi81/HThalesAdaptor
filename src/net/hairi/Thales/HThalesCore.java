package net.hairi.Thales;

import org.jpos.iso.ISOException;
import org.jpos.iso.packager.DummyPackager;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

/*
 * HThalesAdaptor (http://www.m-sinergi.com/hairi/HThalesAdaptor)
 * A contribution to the
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2011 Hairi (hairi@m-sinergi.com)
 *
 */
public class HThalesCore {
                          
      private HThalesChannel channel;
      String basePath;



     public HThalesCore(String host, int port, String basePath, String schema) {
         channel = new HThalesChannel();
         channel.setHost(host, port);
         channel.setPackager(new DummyPackager());
         this.basePath=basePath;

         channel.setBasePath(basePath);

         channel.setSchema(schema);
         

     }
     public void connect() throws IOException {
         channel.connect();
     }


     public boolean isConnected()  {

         return channel.isConnected();


     }

     public void disconnect() throws IOException {


         channel.disconnect();
         
     }



     public HThalesMsg createRequest(String command) throws IOException {




         HThalesMsg req = new HThalesMsg("file:"+basePath);



         
       //Warning, this contain a rough hack
       //Somehow JPOS HThalesMsg will generate a stackoverflow if your schema is not present
       //Can't let that happening to a production system.

         if (command != null)
         {     req.set("command", command);
             File f = null;
             f = new File(new URL(req.getBasePath()+command+".xml").getFile());
        //   System.out.println(f.getAbsolutePath());


             if(!f.exists())
                 throw new IOException("Schema File not defined");

         }

//      req.dump(System.out, "inside createRequest");
         

         return req;
     }


     public HThalesMsg createResponse(String response) {
         HThalesMsg resp = new HThalesMsg("file:"+basePath+"resp-");

         if (response != null)
             resp.set("response", response);

         return resp;
     }

     public HThalesMsg diagnostics() throws IOException {
         return createRequest("NC");
     }

    public HThalesMsg echoTest() throws IOException {



          HThalesMsg req =  createRequest("B2");

          req.set("length", "8");

          Random rnd = new Random();
          int i =  rnd.nextInt(99999999);

           req.set("data", Integer.toString(i));

            return req;


    }




    public void sendKeepAlive() throws Exception
    {
        
       command(echoTest());


    }

     public HThalesMsg generateDoubleLengthKey() throws IOException {
         HThalesMsg req = createRequest("A0");
         req.set("mode", "0");
         req.set("key-type", "001");
         req.set("key-scheme-lmk", "X");

         return req;
     }
                                            
     public HThalesMsg importDoubleLengthKey(String zmk, String key) throws IOException {
         HThalesMsg req = createRequest("A6");
         req.set("key-type", "001");
         req.set("zmk", "X" + zmk);
         req.set("key-under-zmk", "X" + key);
         req.set("key-scheme", "X");

         return req;
     }

     public synchronized HThalesMsg command(HThalesMsg request) throws ISOException, IOException,InterruptedException {

        
    //      System.out.println("Sending Message");
         

         StringBuffer sbuffer = new StringBuffer(request.get("command"));
         sbuffer.setCharAt(1, (char) (sbuffer.charAt(1) + 1));

         try {

             request.pack();

         }  catch(Exception e) {System.out.println("Exception 1e"+e);}


         HThalesMsg resp;


         resp = createResponse(sbuffer.toString());



         HThalesISOMsg msg = new HThalesISOMsg(request);
         msg.setHeader(new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4});

         channel.setBasePath(resp.getBasePath());


         channel.setSchema(resp.getBaseSchema());

        // Thread.sleep(10000);

         try {
             msg.pack();

         } catch(Exception e) {System.out.println("Exception 2e "+e); }

  //       System.out.println("Before send");
         
       
         channel.send(msg);

         HThalesISOMsg response = (HThalesISOMsg) channel.receive();

     //    response.getFSDMsg().dump(System.out, "dump before merge");
         

         resp.merge(response.getFSDMsg());


//      System.out.println("After Merge========");

         return resp;
     }

  
}