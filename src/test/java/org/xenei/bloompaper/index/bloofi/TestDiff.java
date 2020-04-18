//package org.xenei.bloompaper.index.bloofi;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Set;
//
//import org.apache.commons.collections4.bloomfilter.hasher.Shape;
//import org.apache.commons.collections4.bloomfilter.hasher.function.Murmur128x86Cyclic;
//import org.junit.Before;
//import org.junit.Test;
//import org.xenei.bloompaper.RandomBloomFilter;
//import org.xenei.bloompaper.index.NumericBloomFilter;
//import org.xenei.bloompaper.index.hamming.Node;
//
//public class TestDiff {
//    int n = 3;
//    double p = 1.0 / 100000;
//    Shape shape = new Shape(new Murmur128x86Cyclic(), n, p);
//    Bloofi bloofi;
//
////    NumericBloomFilter filter = new NumericBloomFilter( shape, 2315448351954444800L, 34 );
////    NumericBloomFilter target = new NumericBloomFilter( shape, -5570068171581312439L, 34  );
//  NumericBloomFilter filter = new NumericBloomFilter( shape, 5764629513350676800L );
//  NumericBloomFilter target = new NumericBloomFilter( shape, -3025718536694661252L  );
//
//  @Before
//  public void setup() {
//      bloofi = new Bloofi( 10000, shape );
//  }
//
//  @Test
//  public void lo() {
//      NumericBloomFilter filter = new NumericBloomFilter( shape, 5764629513350676800L );
//      NumericBloomFilter target = new NumericBloomFilter( shape, -3025718536694661252L  );
//
//      assertTrue( "bad setup", target.contains( filter ));
//
//      for (int i=0;i<1;i++) {
//          bloofi.add( new RandomBloomFilter(shape));
//      }
//      bloofi.add( target );
//      for (int i=0;i<1;i++) {
//          bloofi.add( new RandomBloomFilter(shape));
//      }
//      Set<NumericBloomFilter> filters = new HashSet<NumericBloomFilter>();
//      bloofi.setFilters(filters);
//      System.out.println( bloofi.count( filter ) );
//      assertFalse( filters.isEmpty() );
//      for (NumericBloomFilter f : filters)
//      {
//          System.out.println( String.format( "%s : %s", f, f.hashCode()  ));
//      }
//      assertFalse( "Should not contain filter", filters.contains( filter ));
//      assertTrue( "Should contain target", filters.contains( target ) );
//  }
//
//}
