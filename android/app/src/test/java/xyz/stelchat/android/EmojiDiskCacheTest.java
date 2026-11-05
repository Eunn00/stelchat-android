package xyz.stelchat.android;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EmojiDiskCacheTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private static final byte[] PNG=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aOioAAAAASUVORK5CYII=");
 private static String name(int i){return String.format("%064d.png",i);}
 @Test public void restartAndCorruption()throws Exception{
  File dir=temp.newFolder();AtomicInteger calls=new AtomicInteger();EmojiDiskCache.Downloader d=n->{calls.incrementAndGet();return PNG;};
  EmojiDiskCache cache=new EmojiDiskCache(dir,10000,d);assertArrayEquals(PNG,cache.get(name(1)));
  EmojiDiskCache offline=new EmojiDiskCache(dir,10000,n->{throw new IOException("offline");});assertArrayEquals(PNG,offline.get(name(1)));
  try(OutputStream out=new FileOutputStream(new File(dir,name(1)))){out.write(new byte[20]);}
  assertArrayEquals(PNG,cache.get(name(1)));assertEquals(2,calls.get());
 }
 @Test public void limitsAndClear()throws Exception{
  EmojiDiskCache c=new EmojiDiskCache(temp.newFolder(),PNG.length*2,n->PNG);
  c.get(name(1));c.get(name(2));c.get(name(3));assertTrue(c.stats()[0]<=PNG.length*2*.8);c.clear();assertEquals(0,c.stats()[0]);
  assertFalse(EmojiDiskCache.valid("../secrets"));
  EmojiDiskCache bad=new EmojiDiskCache(temp.newFolder(),1000,n->new byte[20]);try{bad.get(name(1));fail();}catch(Exception expected){}
 }
 @Test public void simultaneousRequestsAndClear()throws Exception{
  AtomicInteger calls=new AtomicInteger();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  EmojiDiskCache c=new EmojiDiskCache(temp.newFolder(),10000,n->{calls.incrementAndGet();entered.countDown();release.await();return PNG;});
  ExecutorService workers=Executors.newFixedThreadPool(2);
  try{Future<byte[]> first=workers.submit(()->c.get(name(1)));entered.await();c.clear();release.countDown();assertArrayEquals(PNG,first.get());assertEquals(0,c.stats()[0]);}finally{workers.shutdownNow();}
 }
 @Test public void sizeAndDiskFailure()throws Exception{
  File notDirectory=temp.newFile();EmojiDiskCache c=new EmojiDiskCache(notDirectory,10000,n->PNG);assertArrayEquals(PNG,c.get(name(1)));
  EmojiDiskCache big=new EmojiDiskCache(temp.newFolder(),10000,n->new byte[(int)EmojiDiskCache.MAX_FILE+1]);try{big.get(name(2));fail();}catch(Exception expected){}assertEquals(0,big.stats()[0]);
 }
 @Test public void concurrencyAndDuplicate()throws Exception{
  AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger(),calls=new AtomicInteger();
  EmojiDiskCache c=new EmojiDiskCache(temp.newFolder(),10000,n->{calls.incrementAndGet();int current=active.incrementAndGet();peak.accumulateAndGet(current,Math::max);try{Thread.sleep(20);return PNG;}finally{active.decrementAndGet();}});
  ExecutorService workers=Executors.newFixedThreadPool(8);
  try{
   List<Future<byte[]>> results=new ArrayList<>();for(int i=0;i<8;i++)results.add(workers.submit(()->c.get(name(1))));for(Future<byte[]> f:results)assertArrayEquals(PNG,f.get());assertEquals(1,calls.get());
   results.clear();for(int i=2;i<10;i++){final int id=i;results.add(workers.submit(()->c.get(name(id))));}for(Future<byte[]> f:results)f.get();assertTrue(peak.get()<=2);
  }finally{workers.shutdownNow();}
 }
}
