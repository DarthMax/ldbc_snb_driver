/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.                                                                                                                             
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
package com.yahoo.ycsb.workloads;

import java.util.Properties;

import com.google.common.collect.Range;
import com.yahoo.ycsb.WorkloadException;
import com.yahoo.ycsb.Client;
import com.yahoo.ycsb.generator.Generator;
import com.yahoo.ycsb.generator.GeneratorFactory;

/**
 * A disk-fragmenting workload.
 * <p>
 * Properties to control the client:
 * </p>
 * <UL>
 * <LI><b>disksize</b>: how many bytes of storage can the disk store? (default
 * 100,000,000)
 * <LI><b>occupancy</b>: what fraction of the available storage should be used?
 * (default 0.9)
 * <LI><b>requestdistribution</b>: what distribution should be used to select
 * the records to operate on - uniform, zipfian or latest (default: histogram)
 * </ul>
 * 
 * 
 * <p>
 * See also: Russell Sears, Catharine van Ingen. <a href=
 * 'https://database.cs.wisc.edu/cidr/cidr2007/papers/cidr07p34.pdf'>Fragmentati
 * o n in Large Object Repositories</a>, CIDR 2006. [<a href=
 * 'https://database.cs.wisc.edu/cidr/cidr2007/slides/p34-sears.ppt'>Presentatio
 * n < / a > ]
 * </p>
 * 
 * 
 * @author sears
 * 
 */
public class ConstantOccupancyWorkload extends CoreWorkload
{
    long disksize;
    long storageages;
    Generator<Integer> objectsizes;
    double occupancy;

    long object_count;

    public static final String STORAGE_AGE_PROPERTY = "storageages";
    public static final long STORAGE_AGE_PROPERTY_DEFAULT = 10;

    public static final String DISK_SIZE_PROPERTY = "disksize";
    public static final long DISK_SIZE_PROPERTY_DEFAULT = 100 * 1000 * 1000;

    public static final String OCCUPANCY_PROPERTY = "occupancy";
    public static final double OCCUPANCY_PROPERTY_DEFAULT = 0.9;

    @Override
    public void init( Properties p, GeneratorFactory generatorFactory ) throws WorkloadException
    {
        super.init( p, generatorFactory );

        disksize = Long.parseLong( p.getProperty( DISK_SIZE_PROPERTY, DISK_SIZE_PROPERTY_DEFAULT + "" ) );
        storageages = Long.parseLong( p.getProperty( STORAGE_AGE_PROPERTY, STORAGE_AGE_PROPERTY_DEFAULT + "" ) );
        occupancy = Double.parseDouble( p.getProperty( OCCUPANCY_PROPERTY, OCCUPANCY_PROPERTY_DEFAULT + "" ) );

        if ( p.getProperty( Client.RECORD_COUNT ) != null || p.getProperty( Client.INSERT_COUNT ) != null
             || p.getProperty( Client.OPERATION_COUNT ) != null )
        {
            System.err.println( "Warning: record, insert or operation count was set prior to initting ConstantOccupancyWorkload.  Overriding old values." );
        }

        Distribution fieldLengthDistribution = Distribution.valueOf( p.getProperty(
                CoreWorkloadProperties.FIELD_LENGTH_DISTRIBUTION,
                CoreWorkloadProperties.FIELD_LENGTH_DISTRIBUTION_DEFAULT ).toUpperCase() );
        int fieldLength = Integer.parseInt( p.getProperty( CoreWorkloadProperties.FIELD_LENGTH,
                CoreWorkloadProperties.FIELD_LENGTH_DEFAULT ) );
        String fieldLengthHistogramFilePath = p.getProperty( CoreWorkloadProperties.FIELD_LENGTH_HISTOGRAM_FILE,
                CoreWorkloadProperties.FIELD_LENGTH_HISTOGRAM_FILE_DEFAULT );
        Generator<Long> g = WorkloadUtils.buildFieldLengthGenerator( fieldLengthDistribution,
                Range.closed( (long) 1, (long) fieldLength ), fieldLengthHistogramFilePath );
        // TODO is mean() necessary? set to constant for now, fix later
        double fieldsize = fieldLength / 2;
        // double fieldsize = g.mean();

        int fieldcount = Integer.parseInt( p.getProperty( CoreWorkloadProperties.FIELD_COUNT,
                CoreWorkloadProperties.FIELD_COUNT_DEFAULT ) );

        object_count = (long) ( occupancy * ( (double) disksize / ( fieldsize * (double) fieldcount ) ) );
        if ( object_count == 0 )
        {
            throw new IllegalStateException( "Object count was zero.  Perhaps disksize is too low?" );
        }
        p.setProperty( Client.RECORD_COUNT, object_count + "" );
        p.setProperty( Client.OPERATION_COUNT, ( storageages * object_count ) + "" );
        p.setProperty( Client.INSERT_COUNT, object_count + "" );
    }
}
