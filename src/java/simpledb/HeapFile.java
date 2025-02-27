package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {
    File f;
    TupleDesc td;
    int id;
    ArrayList<Integer> page_status = new ArrayList<>();
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.f = f;
        this.td = td;
        this.id = f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return this.f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return this.td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        try (RandomAccessFile file = new RandomAccessFile(this.f, "r")){
            file.seek(pid.getPageNumber() * BufferPool.getPageSize());
            byte[] data = new byte[BufferPool.getPageSize()];
            file.read(data);
            HeapPage hp = new HeapPage((HeapPageId)pid, data);
            return hp;
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(f, "rw")) {
            file.seek(page.getId().getPageNumber() * BufferPool.getPageSize());
            file.write(page.getPageData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return (int)this.f.length()/BufferPool.getPageSize();
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        for(int i = 0; i < page_status.size(); i++) {
            if(page_status.get(i) == 0) {
                // page has free slot
                HeapPageId id = new HeapPageId(this.getId(), i);
                HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, id, Permissions.READ_WRITE);
                page.insertTuple(t);
                if(page.getNumEmptySlots() == 0) {
                    page_status.set(i, 1);
                }
                this.writePage(page);
                ArrayList<Page> p = new ArrayList<>();
                p.add(page);
                return p;
            }
        }
        // create page
        byte[] data = HeapPage.createEmptyPageData();
        HeapPageId id = new HeapPageId(this.getId(), page_status.size());
        HeapPage page = new HeapPage(id, data);
        this.writePage(page);
        page = (HeapPage) Database.getBufferPool().getPage(tid, id, Permissions.READ_WRITE);
        page.insertTuple(t);
        int status = page.getNumEmptySlots() != 0 ? 0 : 1;
        page_status.add(status);
        ArrayList<Page> p = new ArrayList<>();
        p.add(page);
        return p;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
    TransactionAbortedException {
        try {
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, t.getRecordId().getPageId(), Permissions.READ_WRITE);
            page.deleteTuple(t);
            ArrayList<Page> p = new ArrayList<>();
            p.add(page);
            return p;
        } catch (DbException e) {
            throw e;
        }

    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        DbFileIterator it = new HeapFileIterator(tid, new HeapPageId(this.getId(), 0), this);
        return it;
    }

}

