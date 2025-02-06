package simpledb;

import java.util.NoSuchElementException;
import java.util.*;

public class HeapFileIterator implements DbFileIterator{
    TransactionId tid;
    PageId pid;
    HeapFile f;
    HeapPage hp;
    Iterator<Tuple> it;
    boolean itChange = false;

    public HeapFileIterator(TransactionId tid, PageId pid, HeapFile f){
        this.tid = tid;
        this.pid = pid;
        this.f = f;
    }
    @Override
    public void open() throws DbException, TransactionAbortedException {
        this.hp = (HeapPage)Database.getBufferPool().getPage(this.tid, this.pid, Permissions.READ_ONLY);
        this.it = this.hp.iterator();
    }

    @Override
    public boolean hasNext() throws DbException, TransactionAbortedException {
        if(this.it == null) {
            throw new IllegalStateException("Iterator null");
        }
        if(this.it.hasNext()){
            return true;
        }
        if(this.hp.pid.getPageNumber()+1 > this.f.numPages()-1){
            return false;
        }
        HeapPageId hId = new HeapPageId(this.f.getId(), this.hp.pid.getPageNumber()+1);
        HeapPage nextHP = (HeapPage)Database.getBufferPool().getPage(this.tid, hId, Permissions.READ_ONLY);
        Iterator<Tuple> newIt = nextHP.iterator();
        if(!newIt.hasNext()){
            return false;
        }else{
            itChange = true;
            return true;
        }
    }

    @Override
    public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
        
        if (this.hasNext() && !itChange) {
            return this.it.next();
        } else if (this.hasNext() && itChange) {
            HeapPageId hId = new HeapPageId(this.f.getId(), this.hp.pid.getPageNumber() + 1);
            HeapPage nextHP = (HeapPage)Database.getBufferPool().getPage(this.tid, hId, Permissions.READ_WRITE);
            Iterator<Tuple> newIt = nextHP.iterator();
            this.it = newIt;
            this.hp = nextHP;
            itChange = false;
            return this.it.next();
        } else {
            return null;
        }
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        open();
    }

    @Override
    public void close() {
        this.it = null;
    }
    
    
}
