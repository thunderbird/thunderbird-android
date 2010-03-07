/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2001 Lapo Luchini.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;
import java.io.*;

public class ZOutputStream extends OutputStream {

  protected ZStream z=new ZStream();
  protected int bufsize=512;
  protected int flush=JZlib.Z_NO_FLUSH;
  protected byte[] buf=new byte[bufsize],
                   buf1=new byte[1];
  protected boolean compress;

  protected OutputStream out;

  public ZOutputStream(OutputStream out) {
    super();
    this.out=out;
    z.inflateInit();
    compress=false;
  }

  public ZOutputStream(OutputStream out, int level) {
    this(out, level, false);
  }
  public ZOutputStream(OutputStream out, int level, boolean nowrap) {
    super();
    this.out=out;
    z.deflateInit(level, nowrap);
    compress=true;
  }

  public void write(int b) throws IOException {
    buf1[0]=(byte)b;
    write(buf1, 0, 1);
  }

  public void write(byte b[], int off, int len) throws IOException {
    if(len==0)
      return;
    int err;
    z.next_in=b;
    z.next_in_index=off;
    z.avail_in=len;
    do{
      z.next_out=buf;
      z.next_out_index=0;
      z.avail_out=bufsize;
      if(compress)
        err=z.deflate(flush);
      else
        err=z.inflate(flush);
      if(err!=JZlib.Z_OK)
        throw new ZStreamException((compress?"de":"in")+"flating: "+z.msg);
      out.write(buf, 0, bufsize-z.avail_out);
    } 
    while(z.avail_in>0 || z.avail_out==0);
  }

  public int getFlushMode() {
    return(flush);
  }

  public void setFlushMode(int flush) {
    this.flush=flush;
  }

  public void finish() throws IOException {
    int err;
    do{
      z.next_out=buf;
      z.next_out_index=0;
      z.avail_out=bufsize;
      if(compress){ err=z.deflate(JZlib.Z_FINISH);  }
      else{ err=z.inflate(JZlib.Z_FINISH); }
      if(err!=JZlib.Z_STREAM_END && err != JZlib.Z_OK)
      throw new ZStreamException((compress?"de":"in")+"flating: "+z.msg);
      if(bufsize-z.avail_out>0){
	out.write(buf, 0, bufsize-z.avail_out);
      }
    }
    while(z.avail_in>0 || z.avail_out==0);
    flush();
  }
  public void end() {
    if(z==null)
      return;
    if(compress){ z.deflateEnd(); }
    else{ z.inflateEnd(); }
    z.free();
    z=null;
  }
  public void close() throws IOException {
    try{
      try{finish();}
      catch (IOException ignored) {}
    }
    finally{
      end();
      out.close();
      out=null;
    }
  }

  /**
   * Returns the total number of bytes input so far.
   */
  public long getTotalIn() {
    return z.total_in;
  }

  /**
   * Returns the total number of bytes output so far.
   */
  public long getTotalOut() {
    return z.total_out;
  }

  public void flush() throws IOException {
    out.flush();
  }

}
