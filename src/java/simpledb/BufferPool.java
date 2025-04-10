package simpledb;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    ConcurrentHashMap<PageId, Page> mp;
    ConcurrentHashMap<PageId, Integer> e;
    Integer bufferSize;
    LockManager lockmgr;
    /** Bytes per page, including header. */
    private static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;

    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    /** TODO for Lab 4: uncomment declaration of Lock Manager instance. 
    Be sure to instantiate it in the constructor. */
    // private final LockManager lockmgr;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.mp = new ConcurrentHashMap<>();
        this.e = new ConcurrentHashMap<>();
        this.bufferSize = numPages;
        this.lockmgr = new LockManager();
    }

    public static int getPageSize() {
        return pageSize;
    }

    /**
     * Helper: this should be used for testing only!!!
     */
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }
    /**
     * Helper: this should be used for testing only!!!
     */
    public static void resetPageSize(int pageSize) {
        BufferPool.pageSize = PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        try {
            lockmgr.acquireLock(tid, pid, perm);
        } catch (DeadlockException e){
            throw new TransactionAbortedException();
        }
        if(this.mp.get(pid) == null){
            if (mp.size() >= this.bufferSize){
                evictPage();
            }
            Catalog catalog = Database.getCatalog();
            int tableId = pid.getTableId();
            Page newPage = catalog.getDatabaseFile(tableId).readPage(pid);
            this.mp.put(pid, newPage);
            this.e.put(pid, 1);
            e.replaceAll((key, value) -> value + 1);
        }
        return this.mp.get(pid);
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        lockmgr.releaseLock(tid,pid); // Uncomment for Lab 4
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        transactionComplete(tid,true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        return lockmgr.holdsLock(tid, p); // Uncomment for Lab 4 (and remove "return false");
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // TODO: some code goes here
        // not necessary for labs 1--3

        // After dealing with commit/abort actions, ask lock manager to release locks
        // lockmgr.releaseAllLocks(tid); // Uncomment for Lab 4

    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
                HeapFile file = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
                ArrayList<Page> pages = file.insertTuple(tid, t);
                for (Page page : pages) {
                    page.markDirty(true, tid);
                    this.mp.put(t.getRecordId().getPageId(), page);
                }
    }

    /**
     * Remove the specified tuple from the buffer pool by using its RecordId.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
                HeapPageId id = (HeapPageId) t.getRecordId().getPageId();
                
            HeapFile file = (HeapFile) Database.getCatalog().getDatabaseFile(id.getTableId());
            ArrayList<Page> pages = file.deleteTuple(tid, t);
            for (Page page : pages) {
                page.markDirty(true, tid);
                this.mp.put(t.getRecordId().getPageId(), page);
            }
    }

    /**
     * Flush all dirty pages to disk. This method is only used by testing code.
     * Be careful using this method -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        for(PageId key : mp.keySet()) {
            flushPage(key);
        }
    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
     */
    public synchronized void discardPage(PageId pid) {
        // TODO: some code goes here
        // not necessary for labs 1--4
    }

    /**
     * Flushes (i.e., writes) a certain page to disk by asking 
     * the correct HeapFile to write the page
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        Page page = this.mp.get(pid);
        if(page == null) {
            return;
        }
        TransactionId tid = page.isDirty();
        if(tid != null) {
            try {
                DbFile f = Database.getCatalog().getDatabaseFile(pid.getTableId());
                page.markDirty(false, tid);
                f.writePage(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // TODO: some code goes here
        // not necessary for labs 1--4
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        int max = Integer.MIN_VALUE;
        try {
            PageId pageToEvict = null;
            for (PageId key : mp.keySet()) {
                if(e.get(key) > max) {
                    pageToEvict = key;
                    max = e.get(key);
                }
            }
            mp.remove(pageToEvict);
            e.remove(pageToEvict);
            flushPage(pageToEvict);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
