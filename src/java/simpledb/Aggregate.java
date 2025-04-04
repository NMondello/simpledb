package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    DbIterator child;
    DbIterator it;
    int afield;
    int gfield;
    Aggregator.Op aop;
    Aggregator ag;

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of fetchNext().
     * 
     * 
     * @param child
     *            The DbIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(DbIterator child, int afield, int gfield, Aggregator.Op aop) {
        Type gfieldType = null;

        this.child = child;
        this.afield = afield;   
        this.gfield = gfield;
        this.aop = aop;

        if(gfield != -1) {
            gfieldType = child.getTupleDesc().getFieldType(gfield);
        }
        
        if(child.getTupleDesc().getFieldType(afield) == Type.INT_TYPE) {
            this.ag = new IntegerAggregator(gfield, gfieldType, afield, aop);
        } else if(child.getTupleDesc().getFieldType(afield) == Type.STRING_TYPE) {
            this.ag = new StringAggregator(gfield, gfieldType, afield, aop);
        }
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
        if(this.gfield == -1) {
            return Aggregator.NO_GROUPING;
        }
        return this.gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples If not, return
     *         null;
     * */
    public String groupFieldName() {
        if(this.gfield == -1) {
            return null;
        }
        return this.child.getTupleDesc().getFieldName(this.gfield);
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
        return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
        return this.child.getTupleDesc().getFieldName(this.afield);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
        return this.aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
    TransactionAbortedException {
        this.child.open();
        super.open();

        while(this.child.hasNext()) {
            this.ag.mergeTupleIntoGroup(this.child.next());
        }
        this.it = ag.iterator();
        this.it.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate, If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     *
     * Hint: think about how many tuples you have to process from the child operator
     * before this method can return its first tuple.
     * Hint: notice that each Aggregator class has an iterator() method
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        while(this.it.hasNext()){
            return this.it.next();
        }
        return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.it.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        Type[] types;
        String[] names;

        String aggregateColumn = "aggName(" + this.ag.toString() + ") (" + this.child.getTupleDesc().getFieldName(afield) + ")";

        if (gfield == -1) {
            types = new Type[] {Type.INT_TYPE};
            names = new String[] {aggregateColumn};
        } else {
            types = new Type[] {this.child.getTupleDesc().getFieldType(gfield), Type.INT_TYPE};
            names = new String[] {this.child.getTupleDesc().getFieldName(gfield), aggregateColumn};
        }
        return new TupleDesc(types, names);
    }

    public void close() {
        this.child.close();
        super.close();
        this.it.close();
    }

    /**
     * See Operator.java for additional notes
     */
    @Override
    public DbIterator[] getChildren() {
        DbIterator[] it = new DbIterator[]{this.child};
        return it;
    }

    /**
     * See Operator.java for additional notes
     */
    @Override
    public void setChildren(DbIterator[] children) {
        if(!children[0].equals(this.child)){
            children[0] = this.child;
        }
    }

}
