package org.xenei.bloompaper;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Verifier {

    private PrintStream o;

    public Verifier( PrintStream o )
    {
        this.o = o;
    }


    public void verify(final List<Stats> table)
    {

        for (Stats.Phase phase : Stats.Phase.values())
        {
            for (Stats.Type type : Stats.Type.values())
            {

                for (int population : Test.POPULATIONS)
                {
                    Map<Long,List<Stats>> report = table.stream()
                            .filter( s -> s.getPopulation() == population )
                            .collect( Collectors.groupingBy( s -> s.getCount( phase, type )));

                    if (report.size() == 1)
                    {
                        display( String.format( "%s %s %s - OK", phase, type, population));
                    }
                    else {

                        err( String.format( "%s %s %s - ERROR", phase, type, population));
                        for (Map.Entry<Long,List<Stats>> e : report.entrySet()) {
                            err( String.format( "count = %s", e.getKey()));
                            for (Stats s : e.getValue())
                            {
                                err( String.format( "     %s (run %s)", s.getName(), s.getRun()));
                            }
                        }
                        display( "" );
                    }


                }
            }
        }
    }

    private void err( String s )
    {
        System.out.println( s );
        if (o!= null)
        {
            o.println( s );
        }
    }
    private void display( String s )
    {
        System.out.println( s );
        if (o!= null)
        {
            o.println( s );
        }
    }
}
