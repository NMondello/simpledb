package simpledb.systemtest;

import java.io.IOException;
import java.util.ArrayList;
import simpledb.*;

import static org.junit.Assert.*;
import org.junit.Test;

public class InsertTest extends SimpleDbTestBase {
    private void validateInsert(int columns, int sourceRows, int destinationRows)
                throws DbException, IOException, TransactionAbortedException {
        // Create the two tables
        ArrayList<ArrayList<Integer>> sourceTuples = new ArrayList<ArrayList<Integer>>();
        HeapFile source = SystemTestUtil.createRandomHeapFile(
                columns, sourceRows, null, sourceTuples);
        assert sourceTuples.size() == sourceRows;
        ArrayList<ArrayList<Integer>> destinationTuples = new ArrayList<ArrayList<Integer>>();
        HeapFile destination = SystemTestUtil.createRandomHeapFile(
                columns, destinationRows, null, destinationTuples);
        assert destinationTuples.size() == destinationRows;

        // Insert source into destination
        TransactionId tid = new TransactionId();

        SeqScan ss = new SeqScan(tid, source.getId(), "");
        Insert insOp = new Insert(tid, ss, destination.getId());


        // DbFileIterator it1 = destination.iterator(tid);
        // it1.open(); // this needed, otherwise theres an override

        // Query q = new Query(insOp, tid);
        // overriding if the iterator is not open
        insOp.open();
        boolean hasResult = false;
        while (insOp.hasNext()) {
            Tuple tup = insOp.next();
            assertFalse(hasResult);
            hasResult = true;
            assertEquals(SystemTestUtil.SINGLE_INT_DESCRIPTOR, tup.getTupleDesc());
            assertEquals(sourceRows, ((IntField) tup.getField(0)).getValue());
        }
        assertTrue(hasResult);
        insOp.close();

        // DbFileIterator it2 = destination.iterator(tid);
        // it2.open();

        // while(it2.hasNext()) {
        //     System.out.println("ENDING VALUES: " + it2.next());
        // }

        // As part of the same transaction, scan the table
        sourceTuples.addAll(destinationTuples);

        SystemTestUtil.matchTuples(destination, tid, sourceTuples);

        // As part of a different transaction, scan the table
        Database.getBufferPool().transactionComplete(tid);
        Database.getBufferPool().flushAllPages();
        SystemTestUtil.matchTuples(destination, sourceTuples);


    }

    // @Test public void testEmptyToEmpty()
    //         throws IOException, DbException, TransactionAbortedException {
    //     validateInsert(3, 0, 0);
    // }

    // @Test public void testEmptyToOne()
    //         throws IOException, DbException, TransactionAbortedException {
    //     validateInsert(8, 0, 1);
    // }

    // @Test public void testOneToEmpty()
    //         throws IOException, DbException, TransactionAbortedException {
    //     validateInsert(3, 1, 0);
    // }

    @Test public void testOneToOne()
            throws IOException, DbException, TransactionAbortedException {
        validateInsert(1, 1, 1);
    }

    /** Make test compatible with older version of ant. */
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(InsertTest.class);
    }
}
