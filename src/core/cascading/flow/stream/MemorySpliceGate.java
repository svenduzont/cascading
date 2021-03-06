/*
 * Copyright (c) 2007-2012 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.flow.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import cascading.flow.FlowProcess;
import cascading.flow.Scope;
import cascading.pipe.Splice;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleHasher;
import cascading.tuple.Tuples;

/**
 *
 */
public abstract class MemorySpliceGate extends SpliceGate
  {
  protected final Map<Duct, Integer> posMap = new IdentityHashMap<Duct, Integer>();

  protected Comparator<Tuple>[] groupComparators;
  protected Comparator<Tuple>[] valueComparators;
  protected TupleHasher groupHasher;

  protected Set<Tuple> keys;
  protected Map<Tuple, Collection<Tuple>>[] keyValues;

  protected MemoryCoGroupClosure closure;

  protected int numIncomingPaths;

  protected final AtomicInteger count = new AtomicInteger( 0 );

  public MemorySpliceGate( FlowProcess flowProcess, Splice splice )
    {
    super( flowProcess, splice );
    }

  @Override
  public void bind( StreamGraph streamGraph )
    {
    super.bind( streamGraph ); // finds allPrevious

    numIncomingPaths = streamGraph.countAllEventingPathsTo( this );

    orderDucts( streamGraph );
    }

  @Override
  public void initialize()
    {
    super.initialize();

    Comparator defaultComparator = (Comparator) flowProcess.newInstance( (String) flowProcess.getProperty( Tuples.DEFAULT_ELEMENT_COMPARATOR ) );

    groupComparators = new Comparator[ orderedPrevious.length ];

    if( splice.isSorted() )
      valueComparators = new Comparator[ orderedPrevious.length ];

    for( Scope incomingScope : incomingScopes )
      {
      int pos = splice.getPipePos().get( incomingScope.getName() );

      // we want the comparators
      Fields groupFields = splice.getKeySelectors().get( incomingScope.getName() );

      if( groupFields.size() == 0 )
        groupComparators[ pos ] = groupFields;
      else
        groupComparators[ pos ] = new SparseTupleComparator( Fields.asDeclaration( groupFields ), defaultComparator );

      groupComparators[ pos ] = splice.isSortReversed() ? Collections.reverseOrder( groupComparators[ pos ] ) : groupComparators[ pos ];

      if( sortFields != null )
        {
        // we want the comparators
        Fields sortFields = splice.getSortingSelectors().get( incomingScope.getName() );
        valueComparators[ pos ] = new SparseTupleComparator( valuesFields[ pos ], sortFields, defaultComparator );

        if( splice.isSortReversed() )
          valueComparators[ pos ] = Collections.reverseOrder( valueComparators[ pos ] );
        }
      }

    groupHasher = defaultComparator != null ? new TupleHasher( defaultComparator, new Comparator[ orderedPrevious.length ] ) : null;

    keys = createKeySet();

    count.set( numIncomingPaths ); // the number of paths incoming
    }

  @Override
  public void prepare()
    {
    super.prepare();

    keyValues = createKeyValuesArray();

    makePosMap( posMap );

    closure = new MemoryCoGroupClosure( flowProcess, splice.getNumSelfJoins(), keyFields, valuesFields );
    }

  protected Comparator getKeyComparator()
    {
    if( groupComparators.length > 0 && groupComparators[ 0 ] != null )
      return groupComparators[ 0 ];

    return new Comparator<Comparable>()
    {
    @Override
    public int compare( Comparable lhs, Comparable rhs )
      {
      return lhs.compareTo( rhs );
      }
    };
    }

  protected Set<Tuple> createKeySet()
    {
    return Collections.synchronizedSet( new TreeSet<Tuple>( getKeyComparator() ) );
    }

  /**
   * This lets us just replace an old map and let the gc cleanup, vs clearing each map
   *
   * @return
   */
  protected Map<Tuple, Collection<Tuple>>[] createKeyValuesArray()
    {
    // Ducts use identity for equality
    Map<Tuple, Collection<Tuple>>[] valueMap = new Map[ orderedPrevious.length ];

    int start = isBlockingStreamed() ? 0 : 1;
    for( int i = start; i < orderedPrevious.length; i++ )
      {
      if( orderedPrevious[ i ] == null ) // only true for local mode
        continue;

      valueMap[ i ] = createTupleMap();
      }

    return valueMap;
    }

  protected Map<Tuple, Collection<Tuple>> createTupleMap()
    {
    return new HashMap<Tuple, Collection<Tuple>>()
    {
    @Override
    public Collection<Tuple> get( Object object )
      {
      object = getDelegatedTuple( object );

      Collection<Tuple> value = super.get( object );

      if( value == null )
        {
        value = new ArrayList<Tuple>();

        super.put( (Tuple) object, value );
        }

      return value;
      }
    };
    }

  protected final Object getDelegatedTuple( Object object )
    {
    if( groupHasher == null )
      return object;

    return new DelegatedTuple( object );
    }

  protected abstract boolean isBlockingStreamed();

  protected class DelegatedTuple extends Tuple
    {
    public DelegatedTuple( Object object )
      {
      elements = (ArrayList<Object>) Tuple.elements( (Tuple) object );
      }

    @Override
    public boolean equals( Object object )
      {
      return compareTo( object ) == 0;
      }

    @Override
    public int compareTo( Object other )
      {
      return groupComparators[ 0 ].compare( this, (Tuple) other );
      }

    @Override
    public int hashCode()
      {
      return groupHasher.hashCode( this );
      }
    }
  }
