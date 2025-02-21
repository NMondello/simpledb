package simpledb;

import java.util.*;

/**
 * Computes some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {
    int gbfield;
    Type gbfieldtype;
    int afield;
    Op what;

    private static final long serialVersionUID = 1L;

    String gbName;
    String agName;
    HashMap<IntField, Integer> s = new HashMap<>();
    HashMap<IntField, Integer> c = new HashMap<>();

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
        this.gbfield=gbfield;
        this.gbfieldtype=gbfieldtype;
        this.afield=afield;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor. See Aggregator.java for more.
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        this.gbName = tup.getTupleDesc().getFieldName(gbfield);
        this.agName = tup.getTupleDesc().getFieldName(afield);
        int val;
        IntField field = new IntField(NO_GROUPING);
        if (this.gbfield == NO_GROUPING){
            val = new IntField(NO_GROUPING).getValue();
        } else {
            field = (IntField)tup.getField(this.gbfield);
            val = ((IntField)tup.getField(this.afield)).getValue();
        }

        if(this.what.equals(Aggregator.Op.MIN)){
            if (s.containsKey(field)){
                if (s.get(field) > val) {
                    s.put(field, val);
                }
            } else {
                s.put(field, val);
            }
        } else if (this.what.equals(Aggregator.Op.MAX)){
            if (s.containsKey(field)){
                if (s.get(field) < val) {
                    s.put(field, val);
                }
            } else {
                s.put(field, val);
            }
        } else if (this.what.equals(Aggregator.Op.SUM)){
            if (s.containsKey(field)){
                s.put(field, val+s.get(field));
            } else {
                s.put(field, val);
            }
            
        } else if (this.what.equals(Aggregator.Op.AVG)){
            if (s.containsKey(field)) {
                s.put(field, val+s.get(field));
                c.put(field, c.get(field) + 1);
            } else {
                s.put(field, val);
                c.put(field, 1);
            }
        } else if (this.what.equals(Aggregator.Op.COUNT)){
            if (s.containsKey(field)) {
                s.put(field, 1+s.get(field));
            } else {
                s.put(field, 1);
            }
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
        ArrayList<Tuple> tuples = new ArrayList<>();
        Type[] fields = {Type.INT_TYPE};
        String[] fieldNames = {this.agName};
        TupleDesc td = new TupleDesc(fields, fieldNames);
        if(this.gbfield == NO_GROUPING) {
            Tuple t = new Tuple(td);
            t.setField(0, new IntField(s.get(new IntField(NO_GROUPING))));
            tuples.add(t);
        } else {
            for (Map.Entry<IntField, Integer> entry : s.entrySet()) {
                td = new TupleDesc(new Type[]{this.gbfieldtype, Type.INT_TYPE}, new String[]{this.gbName, this.agName});
                Tuple t = new Tuple(td);
                Field key = entry.getKey();
                Integer value = entry.getValue();
                Integer count = c.get(key);
                t.setField(0, key);
                if(this.what == Aggregator.Op.AVG) {
                    t.setField(1, new IntField(value/count));
                } else {
                    t.setField(1, new IntField(value));
                }
                tuples.add(t);
            }
        }
        return new TupleIterator(td, tuples);
    }

}
