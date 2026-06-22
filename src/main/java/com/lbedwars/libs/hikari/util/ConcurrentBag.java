package com.lbedwars.libs.hikari.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentBag<T extends ConcurrentBag.IConcurrentBagEntry> implements AutoCloseable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentBag.class);
   private final CopyOnWriteArrayList<T> sharedList;
   private final boolean weakThreadLocals;
   private final ThreadLocal<List<Object>> threadList;
   private final IBagStateListener listener;
   private final AtomicInteger waiters;
   private volatile boolean closed;
   private final SynchronousQueue<T> handoffQueue;

   public ConcurrentBag(IBagStateListener listener) {
      this.listener = listener;
      this.weakThreadLocals = this.useWeakThreadLocals();
      this.handoffQueue = new SynchronousQueue(true);
      this.waiters = new AtomicInteger();
      this.sharedList = new CopyOnWriteArrayList();
      if (this.weakThreadLocals) {
         this.threadList = ThreadLocal.withInitial(() -> new ArrayList(16));
      } else {
         this.threadList = ThreadLocal.withInitial(() -> new FastList(IConcurrentBagEntry.class, 16));
      }

   }

   public T borrow(long timeout, TimeUnit timeUnit) throws InterruptedException {
      List<Object> list = (List)this.threadList.get();

      for(int i = list.size() - 1; i >= 0; --i) {
         Object entry = list.remove(i);
         IConcurrentBagEntry bagEntry = this.weakThreadLocals ? (IConcurrentBagEntry)((WeakReference)entry).get() : (IConcurrentBagEntry)entry;
         if (bagEntry != null && bagEntry.compareAndSet(0, 1)) {
            return (T)bagEntry;
         }
      }

      int waiting = this.waiters.incrementAndGet();

      try {
         for(T bagEntry : this.sharedList) {
            if (bagEntry.compareAndSet(0, 1)) {
               if (waiting > 1) {
                  this.listener.addBagItem(waiting - 1);
               }

               IConcurrentBagEntry var8 = bagEntry;
               return (T)var8;
            }
         }

         this.listener.addBagItem(waiting);
         timeout = timeUnit.toNanos(timeout);

         do {
            long start = ClockSource.currentTime();
            T bagEntry = (T)(this.handoffQueue.poll(timeout, TimeUnit.NANOSECONDS));
            if (bagEntry == null || bagEntry.compareAndSet(0, 1)) {
               IConcurrentBagEntry var9 = bagEntry;
               return (T)var9;
            }

            timeout -= ClockSource.elapsedNanos(start);
         } while(timeout > 10000L);

         Object var17 = null;
         return (T)var17;
      } finally {
         this.waiters.decrementAndGet();
      }
   }

   public void requite(T bagEntry) {
      bagEntry.setState(0);

      for(int i = 0; this.waiters.get() > 0; ++i) {
         if (bagEntry.getState() != 0 || this.handoffQueue.offer(bagEntry)) {
            return;
         }

         if ((i & 255) == 255) {
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(10L));
         } else {
            Thread.yield();
         }
      }

      List<Object> threadLocalList = (List)this.threadList.get();
      if (threadLocalList.size() < 50) {
         threadLocalList.add(this.weakThreadLocals ? new WeakReference(bagEntry) : bagEntry);
      }

   }

   public void add(T bagEntry) {
      if (this.closed) {
         LOGGER.info("ConcurrentBag has been closed, ignoring add()");
         throw new IllegalStateException("ConcurrentBag has been closed, ignoring add()");
      } else {
         this.sharedList.add(bagEntry);

         while(this.waiters.get() > 0 && bagEntry.getState() == 0 && !this.handoffQueue.offer(bagEntry)) {
            Thread.yield();
         }

      }
   }

   public boolean remove(T bagEntry) {
      if (!bagEntry.compareAndSet(1, -1) && !bagEntry.compareAndSet(-2, -1) && !this.closed) {
         LOGGER.warn((String)"Attempt to remove an object from the bag that was not borrowed or reserved: {}", (Object)bagEntry);
         return false;
      } else {
         boolean removed = this.sharedList.remove(bagEntry);
         if (!removed && !this.closed) {
            LOGGER.warn((String)"Attempt to remove an object from the bag that does not exist: {}", (Object)bagEntry);
         }

         ((List)this.threadList.get()).remove(bagEntry);
         return removed;
      }
   }

   public void close() {
      this.closed = true;
   }

   public List<T> values(int state) {
      List<T> list = (List)this.sharedList.stream().filter((e) -> e.getState() == state).collect(Collectors.toList());
      Collections.reverse(list);
      return list;
   }

   public List<T> values() {
      return (List)this.sharedList.clone();
   }

   public boolean reserve(T bagEntry) {
      return bagEntry.compareAndSet(0, -2);
   }

   public void unreserve(T bagEntry) {
      if (bagEntry.compareAndSet(-2, 0)) {
         while(this.waiters.get() > 0 && !this.handoffQueue.offer(bagEntry)) {
            Thread.yield();
         }
      } else {
         LOGGER.warn((String)"Attempt to relinquish an object to the bag that was not reserved: {}", (Object)bagEntry);
      }

   }

   public int getWaitingThreadCount() {
      return this.waiters.get();
   }

   public int getCount(int state) {
      int count = 0;

      for(T e : this.sharedList) {
         if (e.getState() == state) {
            ++count;
         }
      }

      return count;
   }

   public int[] getStateCounts() {
      int[] states = new int[6];

      for(T e : this.sharedList) {
         ++states[e.getState()];
      }

      states[4] = this.sharedList.size();
      states[5] = this.waiters.get();
      return states;
   }

   public int size() {
      return this.sharedList.size();
   }

   public void dumpState() {
      this.sharedList.forEach((entry) -> LOGGER.info(entry.toString()));
   }

   private boolean useWeakThreadLocals() {
      try {
         if (System.getProperty("com.lbedwars.libs.hikari.useWeakReferences") != null) {
            return Boolean.getBoolean("com.lbedwars.libs.hikari.useWeakReferences");
         } else {
            return this.getClass().getClassLoader() != ClassLoader.getSystemClassLoader();
         }
      } catch (SecurityException var2) {
         return true;
      }
   }

   public interface IBagStateListener {
      void addBagItem(int var1);
   }

   public interface IConcurrentBagEntry {
      int STATE_NOT_IN_USE = 0;
      int STATE_IN_USE = 1;
      int STATE_REMOVED = -1;
      int STATE_RESERVED = -2;

      boolean compareAndSet(int var1, int var2);

      void setState(int var1);

      int getState();
   }
}
