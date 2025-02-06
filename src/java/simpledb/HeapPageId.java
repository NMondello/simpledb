package simpledb;

/** Unique identifier for HeapPage objects. */
public class HeapPageId implements PageId {
    private Integer tableId;
    private Integer pgNo;

    /**
     * Constructor. Create a page id structure for a specific page of a
     * specific table.
     *
     * @param tableId The table that is being referenced
     * @param pgNo The page number in that table.
     */
    public HeapPageId(int tableId, int pgNo) {
        this.tableId = tableId;
        this.pgNo = pgNo;
    }

    /** @return the table associated with this PageId */
    public int getTableId() {
        return this.tableId;
    }

    /**
     * @return the page number in the table getTableId() associated with
     *   this PageId
     */
    public int getPageNumber() {
        return this.pgNo;
    }

    /**
     * @return a hash code for this page, represented by the concatenation of
     *   the table number and the page number (needed if a PageId is used as a
     *   key in a hash table in the BufferPool, for example.). You may want to 
     *   parse the concatenation as a long (Long.parseLong()) before casting to int.
     *   Note that it's possible for both the table id and the page number to be negative 
     *   (for testing), so you may need to modify those values before concatenating.
     * @see BufferPool
     */
    public int hashCode() {
        String concat = Math.abs(this.tableId) + "" + Math.abs(this.pgNo);
        long l = Long.parseLong(concat);
        return (int) l;
    }

    /**
     * Compares one PageId to another.
     *
     * @param o The object to compare against (must be a PageId)
     * @return true if the objects are equal (e.g., page numbers and table
     *   ids are the same)
     */
    public boolean equals(Object o) {
        PageId pid;
        if(o instanceof PageId) {
            pid = (PageId) o;
            if(pid.getTableId() != this.getTableId()) {
                return false;
            }
            if(pid.getPageNumber() != this.getPageNumber()){
                return false;
            }
            return true;
        }else{
            return false;
        }
    }

    /**
     *  Return a representation of this object as an array of
     *  integers, for writing to disk.  Size of returned array must contain
     *  number of integers that corresponds to number of args to one of the
     *  constructors.
     */
    public int[] serialize() {
        int data[] = new int[2];

        data[0] = getTableId();
        data[1] = getPageNumber();

        return data;
    }

}
