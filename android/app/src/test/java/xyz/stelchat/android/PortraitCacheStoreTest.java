package xyz.stelchat.android;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.util.Arrays;

public class PortraitCacheStoreTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private String key(char c){char[] chars=new char[64];Arrays.fill(chars,c);return new String(chars);}
 @Test public void exactAndFallbackSurviveRestartAndStaySeparated()throws Exception{
  File root=temp.newFolder();byte[] a=new byte[24],b=new byte[25];
  Arrays.fill(a,(byte)1);Arrays.fill(b,(byte)2);
  String uid="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  PortraitCacheStore.write(root,key('a'),a);PortraitCacheStore.write(root,uid+"-x",a);
  PortraitCacheStore.write(root,uid+"-chzzk",b);
  assertArrayEquals(a,PortraitCacheStore.read(root,key('a')));
  assertArrayEquals(a,PortraitCacheStore.read(root,uid+"-x"));
  assertArrayEquals(b,PortraitCacheStore.read(root,uid+"-chzzk"));
  PortraitCacheStore.write(root,key('a'),b);
  assertArrayEquals(b,PortraitCacheStore.read(root,key('a')));
  assertEquals(3,root.listFiles().length);
 }
 @Test public void invalidOrOversizedFilesCannotEscapeOrLoad()throws Exception{
  File root=temp.newFolder();PortraitCacheStore.write(root,"../outside",new byte[24]);
  assertEquals(0,root.listFiles().length);assertNull(PortraitCacheStore.read(root,"../outside"));
  PortraitCacheStore.write(root,key('a'),new byte[PortraitCacheStore.MAX_BYTES+1]);
  assertNull(PortraitCacheStore.read(root,key('a')));
 }
 @Test public void cacheHasBoundedFileCountAndNoTemporaryLeaks()throws Exception{
  File root=temp.newFolder();
  for(int i=0;i<70;i++)PortraitCacheStore.write(root,String.format("%064x",i),new byte[24]);
  assertTrue(root.listFiles().length<=64);
  for(File file:root.listFiles())assertTrue(PortraitCacheStore.valid(file.getName()));
 }
}
