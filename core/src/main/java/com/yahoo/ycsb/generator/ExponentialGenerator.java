/**                                                                                                                                                                                
 * Copyright (c) 2011 Yahoo! Inc. All rights reserved.                                                                                                                             
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */

package com.yahoo.ycsb.generator;

import org.apache.commons.math3.random.RandomDataGenerator;

import com.yahoo.ycsb.RandomDataGeneratorFactory;
import com.yahoo.ycsb.WorkloadException;

/**
 * Produces a sequence of longs according to an exponential distribution.
 * Smaller intervals are more frequent than larger ones, and there is no bound
 * on the length of an interval.
 * 
 * gamma: mean rate events occur. 1/gamma: half life - average interval length
 */
public class ExponentialGenerator extends Generator<Long>
{
    // % of readings within most recent exponential.frac portion of dataset
    public static final String EXPONENTIAL_PERCENTILE = "exponential.percentile";
    public static final String EXPONENTIAL_PERCENTILE_DEFAULT = "95";

    // Fraction of the dataset accessed exponential.percentile of the time
    public static final String EXPONENTIAL_FRAC = "exponential.frac";
    public static final String EXPONENTIAL_FRAC_DEFAULT = "0.8571428571"; // 1/7

    // Exponential constant
    private double gamma;

    ExponentialGenerator( RandomDataGenerator random, double mean )
    {
        super( random );
        gamma = 1.0 / mean;
    }

    ExponentialGenerator( RandomDataGenerator random, double percentile, double range )
    {
        super( random );
        gamma = -Math.log( 1.0 - percentile / 100.0 ) / range;
    }

    @Override
    protected Long doNext()
    {
        // TODO replace with internal class variable random
        return (long) ( -Math.log( getRandom().nextUniform( 0, 1 ) ) / gamma );
    }

    // public double mean()
    // {
    // return 1.0 / gamma;
    // }

    // TODO is this just a lame test?
    public static void main( String args[] ) throws WorkloadException
    {
        GeneratorFactory gf = new GeneratorFactory( new RandomDataGeneratorFactory( 42l ) );

        ExponentialGenerator e = (ExponentialGenerator) gf.newExponentialGenerator( 90, 100 );
        int j = 0;
        for ( int i = 0; i < 1000; i++ )
        {
            if ( e.next() < 100 )
            {
                j++;
            }
        }
        System.out.println( "Got " + j + " hits.  Expect 900" );
    }
}
