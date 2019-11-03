package net.hairi.Thales;

import org.jpos.iso.FSDISOMsg;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.FSDChannel;
import org.jpos.util.FSDMsg;

/*
 * HThalesAdaptor (http://www.m-sinergi.com/hairi/HThalesAdaptor)
 * A contribution to the
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2011 Hairi (hairi@m-sinergi.com)
 *
 */
public class HThalesChannel extends FSDChannel {
     String basePath;
     String schema;

    String baseResponsePath;

    public String getBaseResponsePath() {
        return baseResponsePath;
    }

    public void setBaseResponsePath(String baseResponsePath) {
        this.baseResponsePath = baseResponsePath;
    }



     public String getBasePath() {
         return basePath;
     }

     public void setBasePath(String basePath) {
         this.basePath = basePath;
     }

     public String getSchema() {
         return schema;
     }

     public void setSchema(String schema) {
         this.schema = schema;
     }

     @Override
     public ISOMsg createMsg() {
         if (basePath != null && schema != null)
             return new HThalesISOMsg(new HThalesMsg(basePath, schema));

         if (basePath != null)
             return new HThalesISOMsg(new HThalesMsg(basePath));

         return new HThalesISOMsg();
     }
}