package simpledb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {
    int min;
    int max;
    int numBuckets;
    int bucketWidth;
    ConcurrentHashMap<Integer,Integer> bucketsMap;
    int numTuples;


    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     *
     * Note: if the number of buckets exceeds the number of distinct integers between min and max, 
     * some buckets may remain empty (don't create buckets with non-integer widths).
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
    	this.bucketsMap = new ConcurrentHashMap<>();
        this.numBuckets = buckets;
        this.min = min;
        this.max = max;
        this.numTuples = 0;
        this.bucketWidth = (int)Math.ceil((max - min + 1) / buckets);
        if(this.bucketWidth == 0) {
            this.bucketWidth = 1;
        }
        for(int i = 1; i <= buckets; i++){
            this.bucketsMap.put(i, 0);
        }
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        int bucketIndex =  (int)Math.floor(((v - min) / this.bucketWidth)) + 1;
        if(bucketIndex > this.numBuckets){
            bucketIndex = this.numBuckets;
        }
        this.numTuples++;
    	this.bucketsMap.put(bucketIndex, this.bucketsMap.get(bucketIndex) + 1);
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        int amountLastBucket = this.bucketWidth;
    	if(op.equals(Predicate.Op.LIKE) || op.equals(Predicate.Op.EQUALS) || op.equals(Predicate.Op.NOT_EQUALS)){
            if((v < this.min || v > this.max)) {
                if(op.equals(Predicate.Op.NOT_EQUALS)) {
                    return 1.0;
                } else {
                    return 0.0;
                }
            }
            int bucketIndex = (int)Math.floor(((v - min) / this.bucketWidth)) + 1;
            if(bucketIndex >= this.numBuckets){
                bucketIndex = this.numBuckets;
                amountLastBucket = this.max - (this.bucketWidth * (this.numBuckets - 1));
            }
            double selectivity = (double)(this.bucketsMap.get(bucketIndex)/amountLastBucket) / this.numTuples;

            if(op.equals(Predicate.Op.NOT_EQUALS)) {
                return 1.0 - selectivity;
            }

            return selectivity;
        }else if (op.equals(Predicate.Op.GREATER_THAN) || op.equals(Predicate.Op.GREATER_THAN_OR_EQ)){
            if(v > this.max) {
                return 0;
            } else if (v < this.min) {
                return 1;
            }
            int bucketIndex =  (int)Math.floor(((v - min) / this.bucketWidth)) + 1;
            if(bucketIndex >= this.numBuckets){
                bucketIndex = this.numBuckets;
                amountLastBucket = this.max - (this.bucketWidth * (this.numBuckets - 1));
            }
            int b_right = (this.bucketWidth * bucketIndex) + (min - 1);
            int h_b = this.bucketsMap.get(bucketIndex);
            double b_f = h_b / this.numTuples;
            int b_part = (b_right - v) / amountLastBucket;
            double selectivity = b_f * b_part;
            for(int i = bucketIndex + 1; i <= this.numBuckets; ++i){
                h_b = this.bucketsMap.get(i);
                selectivity += (double)h_b / this.numTuples;
            }
            if(op.equals(Predicate.Op.GREATER_THAN_OR_EQ)){
                return selectivity + ((double)(this.bucketsMap.get(bucketIndex)/amountLastBucket) / this.numTuples);
            }
            return selectivity;

        }else if (op.equals(Predicate.Op.LESS_THAN) || op.equals(Predicate.Op.LESS_THAN_OR_EQ)){
            if(v < this.min) {
                return 0;
            } else if (v > this.max) {
                return 1;
            }
            int bucketIndex =  (int)Math.floor(((v - min) / this.bucketWidth)) + 1;
            if(bucketIndex >= this.numBuckets){
                bucketIndex = this.numBuckets;
                amountLastBucket = this.max - (this.bucketWidth * (this.numBuckets - 1));
            }
            int b_left = (this.bucketWidth * bucketIndex) - amountLastBucket + this.min;
            int h_b = this.bucketsMap.get(bucketIndex);
            int b_f = h_b / this.numTuples;
            int b_part = b_left - v / amountLastBucket;
            double selectivity = b_f * b_part;
            for(int i = bucketIndex-1; i >= 1; --i){
                h_b = this.bucketsMap.get(i);
                selectivity += (double)h_b / this.numTuples;
            }
            if(op.equals(Predicate.Op.LESS_THAN_OR_EQ)){
                return selectivity + ((double)(this.bucketsMap.get(bucketIndex)/amountLastBucket) / this.numTuples);
            }
            return selectivity;
        }
        return -1.0;
    }
    
    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It could be used to
     *     implement a more efficient optimization
     *
     * Not necessary for lab 3
     * */
    public double avgSelectivity()
    {
        return 0.5;
    }
    
    /**
     * (Optional) A String representation of the contents of this histogram
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        String s = "{";
        
        // Iterate over the map entries
        for (Map.Entry<Integer, Integer> entry : this.bucketsMap.entrySet()) {
            s += entry.getKey() + "=" + entry.getValue() + ", ";
        }

        // Remove the last comma and space if there are entries
        if (s.length() > 1) {
            s = s.substring(0, s.length() - 2);
        }
        
        s += "}";
        return s;
    }
}
