package simpledb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query. 
 * 
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {
    // Note: the constructor you will implement is below these static methods

    HeapFile f;
    TupleDesc td;
    ConcurrentHashMap<Integer, Object> hMap;
    int numTuples;
    int ioCostPerPage;

    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(HashMap<String,TableStats> s)
    {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;

    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     * 
     * @param tableid
     *            The table over which to compute statistics
     * @param ioCostPerPage
     *            The cost per page of IO. This doesn't differentiate between
     *            sequential-scan IO and disk seeks.
     */
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // Note: See project description for hint on using a Transaction

        this.ioCostPerPage = ioCostPerPage;
        this.f = (HeapFile) Database.getCatalog().getDatabaseFile(tableid);
        this.td = this.f.getTupleDesc();
        int numFields = this.td.numFields();
        int numTuples = 0;


        ConcurrentHashMap<Integer, Integer> minMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Integer> maxMap = new ConcurrentHashMap<>();
        this.hMap = new ConcurrentHashMap<>();

        // Loop through all values and get min and max for each one
        try {
            Transaction tr = new Transaction();
            tr.start();
            SeqScan s =  new SeqScan(tr.getId(), tableid, "t");

            s.open();

            while(s.hasNext()) {
                ++numTuples;
                int index = 0;
                Tuple t = s.next();
                Iterator<Field> fieldIterator = t.fields();
                while (fieldIterator.hasNext()) {
                    Field ogField = fieldIterator.next();
                    if(ogField.getType() == Type.INT_TYPE) {
                        IntField field = (IntField)(ogField);
                        int value = field.getValue();
                        if (value < minMap.getOrDefault(index, Integer.MAX_VALUE)) {
                            minMap.put(index, value);
                        }
                        if (value > maxMap.getOrDefault(index, Integer.MIN_VALUE)) {
                            maxMap.put(index, value);
                        }
                    } else {
                        minMap.put(index, 0);
                        maxMap.put(index, 0);
                    }
                    ++index;
                }
            }
            
            tr.commit();

            for(int i = 0; i < numFields; ++i) {
                if(this.td.getFieldType(i) == Type.INT_TYPE) {
                    // IntHisto
                    this.hMap.put(i, new IntHistogram(NUM_HIST_BINS, minMap.get(i), maxMap.get(i)));
                } else {
                    // StringHisto
                    this.hMap.put(i, new StringHistogram(NUM_HIST_BINS));
                }
            }

            Transaction tr2 = new Transaction();
            tr2.start();

            SeqScan s2 = new SeqScan(tr2.getId(), tableid, "t");
            s2.open();

            while(s2.hasNext()) {
                int index = 0;
                Tuple t = s2.next();
                Iterator<Field> fieldIterator = t.fields();
                while (fieldIterator.hasNext()) {
                    Field ogField = fieldIterator.next();
                    if(ogField.getType() == Type.INT_TYPE) {
                        IntField field = (IntField)(ogField);
                        int value = field.getValue();
                        IntHistogram hist = (IntHistogram) this.hMap.get(index);
                        hist.addValue(value);
                    } else { // StringField
                        StringField field = (StringField)(ogField);
                        String value = field.getValue();
                        StringHistogram hist = (StringHistogram) this.hMap.get(index);
                        hist.addValue(value);
                    }
                ++index;
                }
            }

            tr2.commit();
            this.numTuples = numTuples;

        } catch(Exception e) {
            e.printStackTrace();
        }        
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * 
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     * 
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        int pages = this.f.numPages();
        return pages * ioCostPerPage;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     * 
     * @param selectivityFactor
     *            The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        return (int) (this.numTuples * selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     * @param field
     *        the index of the field
     * @param op
     *        the operator in the predicate
     * The semantic of the method is that, given the table, and then given a
     * tuple, of which we do not know the value of the field, return the
     * expected selectivity. You may estimate this value from the histograms.
     *
     * Not necessary for lab 3
     * */
    public double avgSelectivity(int field, Predicate.Op op) {
        // TODO: some code goes here
        return 0.5;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     * 
     * @param field
     *            The field over which the predicate ranges
     * @param op
     *            The logical operation in the predicate
     * @param constant
     *            The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        if(constant.getType() == Type.INT_TYPE) {
            IntHistogram hist = (IntHistogram) this.hMap.get(field);
            IntField c = (IntField) constant;
            return hist.estimateSelectivity(op, c.getValue());
        } else {
            StringHistogram hist = (StringHistogram) this.hMap.get(field);
            StringField c = (StringField) constant;
            return hist.estimateSelectivity(op, c.getValue());
        }
    }

    /**
     * return the total number of tuples in this table
     * */
    public int totalTuples() {
        return this.numTuples;
    }

}
