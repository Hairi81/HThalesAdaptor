/*
 * HThalesAdaptor (http://www.m-sinergi.com/hairi/HThalesAdaptor)
 * A contribution to the
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2011 Hairi (hairi@m-sinergi.com)
 *
 */

package net.hairi.Thales;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jdom.JDOMException;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

public class HThalesISOMsg extends ISOMsg implements Cloneable  {
    HThalesMsg fsd;
    public  HThalesISOMsg () {
        super();
    }
    public HThalesISOMsg (HThalesMsg fsd) {
        super();
        this.fsd = fsd;
    }
    public String getMTI() {
        return getString(0);
    }
    public byte[] pack() throws ISOException {
        try {
            return fsd.packToBytes();
        } catch (Exception e) {
            throw new ISOException (e);
        }
    }
    public int unpack(byte[] b) throws ISOException {
        try {
            fsd.unpack (b);
            return b.length;
        } catch (Exception e) {
            throw new ISOException (e);
        }
    }
    public void unpack (InputStream in) throws IOException, ISOException {
        synchronized (this) {
            try {
                fsd.unpack(in);
            } catch (JDOMException e) {
                throw new ISOException (e);
            }
        }
    }

    public HThalesMsg getFSDMsg() {
        return fsd;
    }
    public String getString (int fldno) {
        return fsd.get (Integer.toString(fldno));
    }
    public boolean hasField (int fldno) {
        return getString(fldno) != null;
    }
    public void dump (PrintStream p, String indent) {
        if (fsd != null)
            fsd.dump (p, indent);
    }
    public void writeExternal (ObjectOutput out) throws IOException {
        out.writeByte (0);  // reserved for future expansion (version id)
        out.writeUTF (fsd.getBasePath());
        out.writeUTF (fsd.getBaseSchema());
        out.writeObject (fsd.getMap());
    }
    public void readExternal  (ObjectInput in) 
        throws IOException, ClassNotFoundException
    {
        in.readByte();  // ignore version for now
        String basePath = in.readUTF();
        String baseSchema = in.readUTF();
        fsd = new HThalesMsg (basePath, baseSchema);
        Map map = (Map) in.readObject();
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            fsd.set ((String) entry.getKey(), (String) entry.getValue());
        }
    }
    public Object clone() {
        HThalesISOMsg m = (HThalesISOMsg) super.clone();
        m.fsd = (HThalesMsg) fsd.clone();
        return m;
    }
    public Object clone(int[] fields) {
        HThalesISOMsg m = (HThalesISOMsg) super.clone();
        m.fsd = new HThalesMsg(fsd.getBasePath(), fsd.getBaseSchema());
        for (int i=0; i<fields.length; i++) {
            String f = Integer.toString(fields[i]);
            m.fsd.set (f, fsd.get (f));
        }
        return m;
    }
    public void merge (ISOMsg m) {
        if (m instanceof HThalesISOMsg) {
            fsd.merge (((HThalesISOMsg)m).getFSDMsg());
        } else {
            for (int i=0; i<=m.getMaxField(); i++) {
                if (m.hasField(i))
                    fsd.set (Integer.toString(i), m.getString(i));
            }
        }
    }
    public void setResponseMTI() {
        try {
            super.setResponseMTI();
        } catch (ISOException ignored) { }               
    }
    private static final long serialVersionUID = 1L;
}

