package simpledb;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.crypto.Data;

/**
 * Manages locks on PageIds held by TransactionIds.
 * S-locks and X-locks are represented as Permissions.READ_ONLY and Permisions.READ_WRITE, respectively
 *
 * All the field read/write operations are protected by this
 */
public class LockManager {
    ConcurrentHashMap<TransactionId, ArrayList<PageId>> tr2pg;
    ConcurrentHashMap<PageId, ArrayList<TransactionId>> pid2tr;
    ConcurrentHashMap<PageId,Permissions> pg2perm;
    final int LOCK_WAIT = 10;       // milliseconds
    /**
     * Sets up the lock manager to keep track of page-level locks for transactions
     * Should initialize state required for the lock table data structure(s)
     */
    public LockManager() {
        tr2pg = new ConcurrentHashMap<>();
        pid2tr = new ConcurrentHashMap<>();
        pg2perm = new ConcurrentHashMap<>();
    }

    /**
     * Tries to acquire a lock on page pid for transaction tid, with permissions perm. 
     * If cannot acquire the lock, waits for a timeout period, then tries again. 
     * This method does not return until the lock is granted (or an exception is thrown)
     *
     * In Exercise 5, checking for deadlock will be added in this method
     * Note that a transaction should throw a DeadlockException in this method to 
     * signal that it should be aborted.
     *
     * @throws DeadlockException after on cycle-based deadlock
     */
    public boolean acquireLock(TransactionId tid, PageId pid, Permissions perm)
            throws DeadlockException {

        while(!lock(tid, pid, perm)) { // while xact doesn't have the lock, try to get the lock

            synchronized(this) {
                // xact doesn't have the lock yet
                // might neeed some code here for Exercise 5, deadlock detection
            }

            try {
                // xact couldn't get lock, wait a bit (via sleep) before trying again
                Thread.sleep(LOCK_WAIT); 
            } catch (InterruptedException e) { // don't need to do anything 
            }

        }

        synchronized(this) {
            // for Exercise 5, might need some cleanup on deadlock detection data structure
        }

        return true; // should only reach here if lock was eventually acquired
    }

    /**
     * Release all locks corresponding to TransactionId tid.
     * This method is used by BufferPool.transactionComplete()
     */
    public synchronized void releaseAllLocks(TransactionId tid) {
        // TODO: some code goes here
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public synchronized boolean holdsLock(TransactionId tid, PageId p) {
        if(tr2pg.getOrDefault(tid, null).contains(p)){
            return true;
        }
        return false;
    }

    /**
     * Answers the question: is this transaction "locked out" of acquiring lock on this page with this perm?
     * Returns false if this tid/pid/perm lock combo can be achieved (i.e., not locked out), true otherwise.
     * 
     * Logic:
     *
     * if perm == READ_ONLY
     *  if tid is holding any sort of lock on pid, then the tid can acquire the lock (return false).
     *
     *  if another tid is holding a READ lock on pid, then the tid can acquire the lock (return false).
     *  if another tid is holding a WRITE lock on pid, then tid can not currently 
     *  acquire the lock (return true).
     *
     * else
     *   if tid is THE ONLY ONE holding a READ lock on pid, then tid can acquire the lock (return false).
     *   if tid is holding a WRITE lock on pid, then the tid already has the lock (return false).
     *
     *   if another tid is holding any sort of lock on pid, then the tid cannot currenty acquire the lock (return true).
     */
    private synchronized boolean locked(TransactionId tid, PageId pid, Permissions perm) {

        if(perm.equals(Permissions.READ_ONLY)) {
            ArrayList<PageId> pages = tr2pg.getOrDefault(tid, new ArrayList<PageId>());
            if(pg2perm.getOrDefault(pid, null) == null){
                return false;
            }
            if (pages.contains(pid)) {
                return false;
            } else if (pg2perm.get(pid).equals(Permissions.READ_ONLY)) {
                return false;
            } else if (pg2perm.get(pid).equals(Permissions.READ_WRITE)) {
                return true;
            }else{
                return false;
            }
        } else {
            ArrayList<TransactionId> tids = pid2tr.getOrDefault(pid, new ArrayList<TransactionId>());
            if(pg2perm.getOrDefault(pid, null) == null){
                return false;
            }
            if (tids.size() == 1 && tids.contains(tid) && pg2perm.get(pid).equals(Permissions.READ_ONLY)) {
                return false;
            } else if (tids.size() != 0 && tids.contains(tid) && pg2perm.get(pid).equals(Permissions.READ_WRITE)) {
                return false;
            } else if (tids.size() != 0 && !tids.contains(tid)) {
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Releases whatever lock this transaction has on this page
     * Should update lock table data structure(s)
     *
     * Note that you do not need to "wake up" another transaction that is waiting for a lock on this page,
     * since that transaction will be "sleeping" and will wake up and check if the page is available on its own
     * However, if you decide to change the fact that a thread is sleeping in acquireLock(), you would have to wake it up here
     */
    public synchronized void releaseLock(TransactionId tid, PageId pid) {
       ArrayList<PageId> pages = tr2pg.getOrDefault(tid, null);
       pages.remove(pid);
       tr2pg.put(tid, pages);
       ArrayList<TransactionId> tids = pid2tr.getOrDefault(pid, null);
       tids.remove(tid);
       pid2tr.put(pid, tids);

       if(tids.isEmpty()){
        pg2perm.remove(pid);
       }

    }

    /**
     * Attempt to lock the given PageId with the given Permissions for this TransactionId
     * Should update the lock table data structure(s) if successful
     *
     * Returns true if the lock attempt was successful, false otherwise
     */
    private synchronized boolean lock(TransactionId tid, PageId pid, Permissions perm) {

        if(locked(tid, pid, perm))
            return false; // this transaction cannot get the lock on this page; it is "locked out"
        ArrayList<PageId> pages = tr2pg.getOrDefault(tid, new ArrayList<PageId>());
        pages.add(pid);
        tr2pg.put(tid, pages);
        ArrayList<TransactionId> tids = pid2tr.getOrDefault(tid, new ArrayList<TransactionId>());
        tids.add(tid);
        pid2tr.put(pid, tids);
        pg2perm.put(pid, perm);
        return true;
    }
}