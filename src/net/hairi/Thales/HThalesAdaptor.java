package net.hairi.Thales;

import org.jdom.Element;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.*;
import org.jpos.q2.QBeanSupport;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.space.TSpace;
import org.jpos.util.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
/*
 * HThalesAdaptor (http://www.m-sinergi.com/hairi/HThalesAdaptor)
 * A contribution to the
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2011 Hairi (hairi@m-sinergi.com)
 *
 */
public class HThalesAdaptor  extends  QBeanSupport implements Runnable, HThalesAdaptorMBean {


       Space sp;
       HThalesCore channel;
       String in, out, ready, reconnect;
       long delay;
       long keepAliveInterval=30000;
       String basePath;
       String host;
       int port;

    

       boolean keepAlive = false;
       boolean ignoreISOExceptions = false;
       int rx, tx, connects;
       long lastTxn = 0l;
       public HThalesAdaptor () {
           super ();
           resetCounters();
       }
       public void initChannel () throws ConfigurationException {
           Element persist = getPersist ();
           sp = grabSpace (persist.getChild ("space"));
           in      = persist.getChildTextTrim ("in");
           out     = persist.getChildTextTrim ("out");

           String s = persist.getChildTextTrim ("reconnect-delay");
           delay    = s != null ? Long.parseLong (s) : 10000; // reasonable default
        //   channel = newChannel (e, getFactory());

           keepAlive = "yes".equalsIgnoreCase (persist.getChildTextTrim ("keep-alive"));

           basePath = persist.getChildTextTrim("basepath");

         if(keepAlive)
         {
             try {
          keepAliveInterval=Integer.parseInt(persist.getChildTextTrim("keep-alive-interval"));


             } catch(Exception e) {keepAliveInterval=30000;}

         }

           ignoreISOExceptions = "yes".equalsIgnoreCase (persist.getChildTextTrim ("ignore-iso-exceptions"));
       

           ready   = getName() + ".ready";
           reconnect = getName() + ".reconnect";
           NameRegistrar.register (getName(), this);

           host=persist.getChildTextTrim("host");
           port=Integer.parseInt(persist.getChildTextTrim("port"));
           basePath=persist.getChildTextTrim("basepath");


           

          channel = new HThalesCore(host, port, basePath,"base");


       }

    public void startService () {
        try {
            initChannel ();
            new Thread (new Sender ()).start ();
          } catch (Exception e) {
            getLog().warn ("error starting service", e);
        }
    }




    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTickInterval(long tickInterval) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getTickInterval() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

        /**
     * @jmx:managed-attribute description="set reconnect delay"
     */
    public synchronized void setReconnectDelay (long delay) {
        getPersist().getChild ("reconnect-delay")
            .setText (Long.toString (delay));
        this.delay = delay;
        setModified (true);
    }
    /**
     * @jmx:managed-attribute description="get reconnect delay"
     */
    public long getReconnectDelay () {
        return delay;
    }

    /**
     * @jmx:managed-attribute description="input queue"
     */
    public synchronized void setInQueue (String in) {
        String old = this.in;
        this.in = in;
        if (old != null)
            sp.out (old, new Object());

        getPersist().getChild("in").setText (in);
        setModified (true);
    }
    /**
     * @jmx:managed-attribute description="input queue"
     */
    public String getInQueue () {
        return in;
    }
    /**
     * @jmx:managed-attribute description="output queue"
     */
    public synchronized void setOutQueue (String out) {
        this.out = out;
        getPersist().getChild("out").setText (out);
        setModified (true);
    }

    public String getOutQueue() {
        return out;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public class Sender implements Runnable {


        long lastKA = new Date().getTime();

         public Sender () {
             super ();
         }
         public void run () {
             Thread.currentThread().setName ("channel-sender-" + in);
             while (running ()){                          
                 try {
                     checkConnection ();
                     if (!running())
                         break;

                     Object o = sp.in (in, delay);


                    boolean space = false;

                     TSpace ts = new TSpace();
                     
                     
                    if(o instanceof TSpace)
                    {


                      ts = (TSpace)o;



                   HThalesMsg m = (HThalesMsg) ts.in ("Request");
                   o=m;

                   space=true;



                    }



                     if (o instanceof HThalesMsg) {

                        // ((FSDMsg)o).dump(System.out, "req adaptor");

                     HThalesMsg resp =   (HThalesMsg)channel.command ((HThalesMsg) o);

                         tx++;
                         sp.rd (ready);

                         rx++;
                         lastTxn = System.currentTimeMillis();

              //          System.out.println("Transaction :"+ getName());
                       if(!space)
                         sp.out (out, resp);
                     else
                       {
                           


                           ts.out ("Response", resp);
                       }

                   //  System.out.println("done with it");
                     }
                     else if (keepAlive && channel.isConnected()&&((new Date().getTime()-lastKA)>keepAliveInterval)) {
                             channel.sendKeepAlive();
                            lastKA=new Date().getTime();
                         

                     }
                 } catch (ISOFilter.VetoException e) {
                     getLog().warn ("channel-sender-"+in, e.getMessage ());
                 } catch (ISOException e) {
                     getLog().warn ("channel-sender-"+in, e.getMessage ());
                     if (!ignoreISOExceptions) {
                         disconnect ();
                     }
                     ISOUtil.sleep (1000); // slow down on errors
                 } catch (Exception e) {
                     getLog().warn ("channel-sender-"+in, e.getMessage ());
                     disconnect ();
                     ISOUtil.sleep (1000);
                 }
             }
         }
     }


    protected void checkConnection () {
          while (running() &&
                  sp.rdp (reconnect) != null)
          {

              getLog().warn("checkConnection HThalesAdaptor");

              ISOUtil.sleep(1000);
          }
          while (running() && !channel.isConnected ()) {
              while (sp.inp (ready) != null)
                  ;
              try {
                  getLog().warn("checkConnection HThalesAdaptor 2");

                  channel.connect ();
              } catch (IOException e) {
                  getLog().warn ("check-connection", e.getMessage ());
              }
              if (!channel.isConnected ())
                  ISOUtil.sleep (delay);
              else
                  connects++;
          }
          if (running() && (sp.rdp (ready) == null))
              sp.out (ready, new Date());
      }


    protected synchronized void disconnect () {
        try {
            while (sp.inp (ready) != null)
                ;
            channel.disconnect ();
        } catch (IOException e) {
            getLog().warn ("disconnect", e);
        }
    }


    public void resetCounters () {
        rx = tx = connects = 0;
        lastTxn = 0l;
    }

    /**
        * @jmx:managed-attribute description="remote host address"
        */
       public synchronized void setHost (String host) {
           setProperty (getProperties ("channel"), "host", host);
           setModified (true);
       }
       /**
        * @jmx:managed-attribute description="remote host address"
        */
       public String getHost () {
           return getProperty (getProperties ("channel"), "host");
       }
       /**
        * @jmx:managed-attribute description="remote port"
        */
       public synchronized void setPort (int port) {
           setProperty (
               getProperties ("channel"), "port", Integer.toString (port)
           );
           setModified (true);
       }
       /**
        * @jmx:managed-attribute description="remote port"
        */
       public int getPort () {
           int port = 0;
           try {
               port = Integer.parseInt (
                   getProperty (getProperties ("channel"), "port")
               );
           } catch (NumberFormatException e) { }
           return port;
       }
       /**
        * @jmx:managed-attribute description="socket factory"
        */
       public synchronized void setSocketFactory (String sFac) {
           setProperty(getProperties("channel"), "socketFactory", sFac);
           setModified(true);
       }


       public String getCountersAsString () {
           StringBuffer sb = new StringBuffer();
           append (sb, "tx=", tx);
           append (sb, ", rx=", rx);
           append (sb, ", connects=", connects);
           sb.append (", last=");
           sb.append(lastTxn);
           if (lastTxn > 0) {
               sb.append (", idle=");
               sb.append(System.currentTimeMillis() - lastTxn);
               sb.append ("ms");
           }
           return sb.toString();
       }
       public int getTXCounter() {
           return tx;
       }
       public int getRXCounter() {
           return rx;
       }
       public int getConnectsCounter () {
           return connects;
       }
       public long getLastTxnTimestampInMillis() {
           return lastTxn;
       }
       public long getIdleTimeInMillis() {
           return lastTxn > 0L ? System.currentTimeMillis() - lastTxn : -1L;
       }
       /**
        * @jmx:managed-attribute description="socket factory"
        */
       public String getSocketFactory() {
           return getProperty(getProperties ("channel"), "socketFactory");
       }

    public boolean isConnected() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void dump (PrintStream p, String indent) {
           p.println (indent + getCountersAsString());
       }
       private Space grabSpace (Element e) {
           return SpaceFactory.getSpace (e != null ? e.getText() : "");
       }

       private void append (StringBuffer sb, String name, int value) {
           sb.append (name);
           sb.append (value);
       }


}
