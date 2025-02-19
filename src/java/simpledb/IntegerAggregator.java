package simpledb;

/**
 * Computes some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {
    int gbfield;
    Type gbfieldtype;
    int afield;
    Op what;
    int aggregate;
    int count;
    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.what=what;
        if (this.what.equals(Aggregator.Op.MIN)){
            this.aggregate = Integer.MAX_VALUE;
        }else if (this.what.equals(Aggregator.Op.MAX)){
            this.aggregate = Integer.MIN_VALUE;
        }else{
            this.aggregate = 0;
        }
        this.gbfield=gbfield;
        this.gbfieldtype=gbfieldtype;
        this.afield=afield;
        this.count = 0;

    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor. See Aggregator.java for more.
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        int val;
        if (this.gbfield == NO_GROUPING){
            val = new IntField(NO_GROUPING).getValue();
        } else {
            val = ((IntField)tup.getField(this.gbfield)).getValue();
        }

        if(this.what.equals(Aggregator.Op.MIN)){
            if (this.aggregate > val){
                this.aggregate = val;
            }
        } else if (this.what.equals(Aggregator.Op.MAX)){
            if (this.aggregate < val){
                this.aggregate = val;
            }    
        } else if (this.what.equals(Aggregator.Op.SUM)){
            this.aggregate += val;
        } else if (this.what.equals(Aggregator.Op.AVG)){
            this.aggregate += val;
            this.count += 1;
        } else if (this.what.equals(Aggregator.Op.COUNT)){
            this.count += 1;  
        }
    }

    /**
     * Returns a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public DbIterator iterator() {
        
    }

}
