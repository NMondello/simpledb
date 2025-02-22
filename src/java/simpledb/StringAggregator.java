package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Computes some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    int gbfield;
    Type gbfieldtype;
    int afield;
    Op what;

    String gbName;
    String agName;
    HashMap<Field, Integer> s = new HashMap<>();
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;

        if(what != Aggregator.Op.COUNT) {
            throw new IllegalArgumentException();
        }

        this.what = what;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        if (gbfield != -1) {
            this.gbName = tup.getTupleDesc().getFieldName(gbfield);
        }
        this.agName = tup.getTupleDesc().getFieldName(afield);

        Field field = new IntField(NO_GROUPING);
        field = tup.getField(this.gbfield);
        
        if(s.containsKey(field)) {
            s.put(field, s.get(field)+1);
        } else {
            s.put(field, 1);
        }
    }

    /**
     * Returns a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
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
            for (Map.Entry<Field, Integer> entry : s.entrySet()) {
                td = new TupleDesc(new Type[]{this.gbfieldtype, Type.INT_TYPE}, new String[]{this.gbName, this.agName});
                Tuple t = new Tuple(td);
                Field key = entry.getKey();
                Integer value = entry.getValue();
                t.setField(0, key);
                t.setField(1, new IntField(value));
                tuples.add(t);
            }
        }
        return new TupleIterator(td, tuples);
    }

}
