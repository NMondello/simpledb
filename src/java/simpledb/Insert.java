package simpledb;

import java.io.IOException;

/**
 * Inserts tuples read from the child operator into the tableid specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    TransactionId tid;
    DbIterator child;
    int tableId; 
    TupleDesc td;
    boolean fetched = false;
    /**
     * Constructor.
     * 
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableid
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t, DbIterator child, int tableid)
            throws DbException {
        
        this.tid = t;
        this.child = child;
        this.tableId = tableid;
        this.td = Database.getCatalog().getTupleDesc(tableid);

        if(!Database.getCatalog().getTupleDesc(tableid).equals(child.getTupleDesc())) {
            throw new DbException("TupleDesc does not match.");
        }
    }

    public TupleDesc getTupleDesc() {
        return new TupleDesc(new Type[]{Type.INT_TYPE});
    }

    public void open() throws DbException, TransactionAbortedException {
        this.child.open();
        super.open();
    }

    public void close() {
        this.child.close();
        super.close();
    }

    /**
     * You can just close and then open the child
     */
    public void rewind() throws DbException, TransactionAbortedException {
        this.child.close();
        this.child.open();
        this.fetched = false;
    }

    /**
     * Inserts tuples read from child into the relation with the tableid specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records (even if there are 0!). 
     * Insertions should be passed through BufferPool.insertTuple() with the 
     * TransactionId from the constructor. An instance of BufferPool is available via 
     * Database.getBufferPool(). Note that insert DOES NOT need to check to see if 
     * a particular tuple is a duplicate before inserting it.
     *
     * This operator should keep track if its fetchNext() has already been called, 
     * returning null if called multiple times.
     * 
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if(this.fetched) {
            return null;
        }

        this.fetched = true;
        int count = 0;

        while(this.child.hasNext()) {
            try {
                Tuple t = this.child.next();
                System.out.println("INSERTING TUPLE " + t);
                Database.getBufferPool().insertTuple(this.tid, this.tableId, t);
                count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        TupleDesc td = new TupleDesc(new Type[]{Type.INT_TYPE});
        Tuple tup = new Tuple(td);
        tup.setField(0, new IntField(count));

        return tup;
    }

    @Override
    public DbIterator[] getChildren() {
        return new DbIterator[]{this.child};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        this.child = children[0];
    }
}
