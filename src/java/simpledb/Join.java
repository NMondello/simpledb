package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {
    JoinPredicate p;
    DbIterator it1;
    DbIterator it2;
    private static final long serialVersionUID = 1L;

    /**
     * Constructor. Accepts two children to join and the predicate to join them
     * on
     * 
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    public Join(JoinPredicate p, DbIterator child1, DbIterator child2) {
        this.p = p;
        this.it1 = child1;
        this.it2 = child2;
    }

    public JoinPredicate getJoinPredicate() {
        return this.p;
    }

    /**
     * @return
     *       the field name of join field1. Should be quantified by
     *       alias or table name. Can be taken from the appropriate child's TupleDesc.
     * */
    public String getJoinField1Name() {
        return this.it1.getTupleDesc().toString();
    }

    /**
     * @return
     *       the field name of join field2. Should be quantified by
     *       alias or table name. Can be taken from the appropriate child's TupleDesc.
     * */
    public String getJoinField2Name() {
        return this.it2.getTupleDesc().toString();
    }

    /**
     * Should return a TupleDesc that represents the schema for the joined tuples. 
     *@see simpledb.TupleDesc#merge(TupleDesc, TupleDesc) for possible
     *      implementation logic.
     */
    public TupleDesc getTupleDesc() {
        TupleDesc td1 = this.it1.getTupleDesc();
        TupleDesc td2 = this.it2.getTupleDesc();
        TupleDesc merged = TupleDesc.merge(td1, td2);
        return merged;
    }

    public void open() throws DbException, NoSuchElementException,
    TransactionAbortedException {
        this.it1.open();
        this.it2.open();
        super.open();
    }

    public void close() {
        this.it1.close();
        this.it2.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.it1.rewind();
        this.it2.rewind();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator later on if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {

        while(this.it1.hasNext()) {
            Tuple t1 = this.it1.next();
            while(this.it2.hasNext()) {
                Tuple t2 = this.it2.next();
                if(this.p.filter(t1, t2)) {
                    int count = 0;
                    Tuple tupleMerged = new Tuple(getTupleDesc());
                    Iterator<Field> itfield1 = t1.fields();
                    while(itfield1.hasNext()){
                        Field f = itfield1.next();
                        tupleMerged.setField(count, f);
                        count++;
                    }
                    Iterator<Field> itfield2 = t2.fields();
                    while(itfield2.hasNext()) {
                        Field f2 = itfield2.next();
                        tupleMerged.setField(count, f2);
                        count++;
                    }
                    this.it1.rewind();
                    
                    return tupleMerged;
                    //Use rewind because we are finding a match and moving it1 foward, but technically there can be another match so we have to rewind
                }
            }
        }
        return null;
    }

    /**
     * See Operator.java for additional notes
     */
    @Override
    public DbIterator[] getChildren() {
        DbIterator[] children = new DbIterator[2];
        children[0] = this.it1;
        children[1] = this.it2;
        return children;
    }

    /**
     * See Operator.java for additional notes
     */
    @Override
    public void setChildren(DbIterator[] children) {
        DbIterator c1 = children[0];
        DbIterator c2 = children[1];
        if (c1 != this.it1) {
            this.it1 = c1;
        }
        if(c2 != this.it2) {
            this.it2 = c2;
        }
    }

}
