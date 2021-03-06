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

package cascading.tuple;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Class TupleEntryIterator provides an efficient Iterator for returning {@link TupleEntry} elements in an
 * underlying {@link Tuple} collection.
 */
public abstract class TupleEntryIterator implements Iterator<TupleEntry>, Closeable
  {
  /** Field entry */
  final TupleEntry entry = new TupleEntry( true );

  /**
   * Constructor TupleEntryIterator creates a new TupleEntryIterator instance.
   *
   * @param fields of type Fields
   */
  public TupleEntryIterator( Fields fields )
    {
    this.entry.fields = fields;
    this.entry.tuple = Tuple.size( fields.size() );
    }

  /**
   * Method getFields returns the fields of this TupleEntryIterator object.
   *
   * @return the fields (type Fields) of this TupleEntryIterator object.
   */
  public Fields getFields()
    {
    return entry.fields;
    }

  /**
   * Method getTupleEntry returns the entry of this TupleEntryIterator object.
   * <p/>
   * Since TupleEntry instances are re-used, this entry will inherit a new Tuple
   * on every {@link #next()} call.
   *
   * @return the entry (type TupleEntry) of this TupleEntryIterator object.
   */
  public TupleEntry getTupleEntry()
    {
    return entry;
    }
  }
