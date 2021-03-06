package scalaj
package collection
package s2j

import java.{lang => jl}
import java.{util => ju}
import scala.{collection => sc}
import scala.collection.{immutable => sci, mutable => scm}

class IteratorWrapper[A](val underlying: sc.Iterator[A]) extends ju.Iterator[A] with ju.Enumeration[A] {
  override def hasNext(): Boolean = underlying.hasNext
  override def next(): A = underlying.next
  override def remove(): Unit = throw new UnsupportedOperationException

  override def hasMoreElements(): Boolean = underlying.hasNext
  override def nextElement(): A = underlying.next
}

abstract class MutableIteratorWrapper[A](override val underlying: sc.Iterator[A]) extends IteratorWrapper(underlying) {
  protected[this] def remove(element: A): Unit

  private var prev: Option[A] = None

  override def remove(): Unit = {
    remove(prev.getOrElse(throw new IllegalStateException))
    prev = None
  }
  override def next(): A = {
    val rv = underlying.next
    prev = Some(rv)
    rv
  }
}

class IterableWrapper[A](val underlying: sc.Iterable[A]) extends ju.AbstractCollection[A] with Serializable {
  override def size(): Int = underlying.size
  override def iterator(): ju.Iterator[A] = new IteratorWrapper(underlying.iterator)
}

class SeqWrapper[A](val underlying: sc.Seq[A]) extends ju.AbstractList[A] with Serializable {
  override def size(): Int = underlying.size
  override def get(index: Int): A = underlying(index)
}

class LinearSeqWrapper[A](val underlying: sci.LinearSeq[A]) extends ju.List[A] with Serializable {
  // Size can be O(N), so we cache the value here (val instead of def)
  override val size: Int = underlying.size

  // This implementation is not ideal, but we don't have much choice
  override def get(index: Int): A = underlying(index)

  override def isEmpty(): Boolean = underlying.isEmpty

  override def contains(e: AnyRef): Boolean = underlying.contains(e)

  override def containsAll(c: ju.Collection[_]): Boolean = {
    var result = true
    val iter = c.iterator
    while (iter.hasNext && result) {
      val e = iter.next
      if (!underlying.contains(e))
        result = false
    }
    result
  }

  override def subList(start: Int, end: Int): ju.List[A] = new LinearSeqWrapper(underlying.slice(start, end))

  override def toArray(): Array[AnyRef] = underlying.toArray[Any].asInstanceOf[Array[AnyRef]]

  override def toArray[T](arr: Array[T with AnyRef]): Array[T with AnyRef] = {
    val n = this.size
    val res =
      if (arr.length >= n)
        arr
      else
        jl.reflect.Array.newInstance(arr.getClass.getComponentType, n).asInstanceOf[Array[T with AnyRef]]
    underlying.copyToArray(res.asInstanceOf[Array[Any]])
    if (res.length > n)
      res(n) = null.asInstanceOf[T with AnyRef]
    res
  }

  override def add(elem: A): Boolean = throw new UnsupportedOperationException
  override def add(index: Int, elem: A): Unit = throw new UnsupportedOperationException
  override def addAll(c: ju.Collection[_ <: A]): Boolean = throw new UnsupportedOperationException
  override def addAll(index: Int, c: ju.Collection[_ <: A]): Boolean = throw new UnsupportedOperationException
  override def set(index: Int, elem: A): A = throw new UnsupportedOperationException
  override def remove(index: Int): A = throw new UnsupportedOperationException
  override def remove(elem: AnyRef): Boolean = throw new UnsupportedOperationException
  override def removeAll(c: ju.Collection[_]): Boolean = throw new UnsupportedOperationException
  override def retainAll(c: ju.Collection[_]): Boolean = throw new UnsupportedOperationException
  override def clear(): Unit = throw new UnsupportedOperationException

  override def indexOf(elem: AnyRef): Int = underlying.indexOf(elem.asInstanceOf[Any])

  override def lastIndexOf(elem: AnyRef): Int = underlying.lastIndexOf(elem.asInstanceOf[Any])

  override def iterator(): ju.Iterator[A] = new IteratorWrapper(underlying.iterator)

  override def listIterator(): ju.ListIterator[A] = listIterator(0)

  override def listIterator(index: Int): ju.ListIterator[A] = new ju.ListIterator[A] {
    var _next: sci.LinearSeq[A] = underlying
    var _prev: sci.LinearSeq[A] = Nil
    var _index: Int = index

    // Advance the listIterator to the desired location
    for (i <- 1 to index) { next() }

    override def hasNext(): Boolean = !_next.isEmpty
    override def hasPrevious(): Boolean = !_prev.isEmpty
    override def next(): A = {
      val result = _next.head
      _prev = result +: _prev
      _next = _next.tail
      _index += 1
      result
    }
    override def previous(): A = {
      val result = _prev.head
      _next = result +: _next
      _prev = _prev.tail
      _index -= 1
      result
    }
    override def nextIndex(): Int = index
    override def previousIndex(): Int = index - 1
    override def set(e: A): Unit = throw new UnsupportedOperationException
    override def add(e: A): Unit = throw new UnsupportedOperationException
    override def remove(): Unit = throw new UnsupportedOperationException
  }

  override def equals(that: Any): Boolean = that match {
    case that: AnyRef if this eq that =>
      true
    case that: ju.List[_] =>
      val it1 = this.iterator()
      val it2 = that.iterator()
      var same = true
      while (it1.hasNext && it2.hasNext && same) {
        same = (it1.next == it2.next)
      }
      same && !(it1.hasNext || it2.hasNext)
    case _ =>
      false
  }

  override def hashCode(): Int = {
    var _hashCode = 1
    val it = this.iterator()
    while (it.hasNext) {
      val e = it.next
      _hashCode = 31*_hashCode + e.##
    }
    _hashCode
  }

  override def toString(): String = {
    val it = this.iterator()
    if (!it.hasNext) {
      "[]"
    } else {
      val sb = new jl.StringBuilder
      sb.append('[')
      sb.append(it.next)
      while (it.hasNext) {
        sb.append(',').append(' ')
        sb.append(it.next)
      }
      sb.append(']')
      sb.toString
    }
  }
}

class MutableSeqWrapper[A](override val underlying: scm.Seq[A]) extends SeqWrapper(underlying) with Serializable {
  override def set(index: Int, element: A): A = {
    val rv = underlying(index)
    underlying(index) = element
    rv
  }
}

class BufferWrapper[A](override val underlying: scm.Buffer[A]) extends MutableSeqWrapper(underlying) with Serializable {
  override def remove(index: Int): A = underlying.remove(index)
  override def add(index: Int, element: A): Unit = underlying.insert(index, element)
}

class SetWrapper[A](val underlying: sc.Set[A]) extends ju.AbstractSet[A] with Serializable {
  override def iterator(): ju.Iterator[A] = new IteratorWrapper(underlying.iterator)
  override def size(): Int = underlying.size
}

class MutableSetWrapper[A](override val underlying: scm.Set[A]) extends SetWrapper(underlying) with Serializable {
  override def add(element: A): Boolean = {
    val s = underlying.size
    underlying += element
    s < underlying.size
  }
  override def iterator(): ju.Iterator[A] = new MutableIteratorWrapper(underlying.iterator) {
    override def remove(element: A): Unit = MutableSetWrapper.this.underlying.remove(element)
  }
}

object MapWrapper {
  class Entry[A, B](key: A, value: B) extends ju.Map.Entry[A, B] {
    override def getKey() = key
    override def getValue() = value
    override def setValue(newValue: B): B = throw new UnsupportedOperationException
    
    override def equals(that: Any): Boolean = that match {
      case that : ju.Map.Entry[_, _] => key == that.getKey && value == that.getValue
      case _ => false
    }
    override def hashCode(): Int =
      (if (key == null) 0 else key.hashCode) ^ (if (value == null) 0 else value.hashCode)
  }
}

class MapWrapper[A, B](val underlying: sc.Map[A, B]) extends ju.AbstractMap[A, B] with Serializable {
  import MapWrapper.Entry

  override def entrySet(): ju.Set[ju.Map.Entry[A, B]] = new ju.AbstractSet[ju.Map.Entry[A, B]] {
    override def iterator(): ju.Iterator[ju.Map.Entry[A, B]] =
      new IteratorWrapper(underlying.iterator.map { case (k, v) => new Entry(k, v) })
    override def size(): Int = underlying.size
  }
}

class MutableMapWrapper[A, B](val underlying: scm.Map[A, B]) extends ju.AbstractMap[A, B] with Serializable {
  import MapWrapper.Entry

  override def put(key: A, value: B): B = underlying.put(key, value).getOrElse(null.asInstanceOf[B])
  override def entrySet(): ju.Set[ju.Map.Entry[A, B]] =
    new ju.AbstractSet[ju.Map.Entry[A, B]] {
      override def iterator(): ju.Iterator[ju.Map.Entry[A, B]] =
        new MutableIteratorWrapper[ju.Map.Entry[A, B]](underlying.iterator.map { case (k, v) => new Entry(k, v) }) {
          override def remove(element: ju.Map.Entry[A, B]): Unit =
            MutableMapWrapper.this.underlying.remove(element.getKey)
        }
      override def size(): Int = underlying.size
    }
}

class MutableMapDictionaryWrapper[A, B](val underlying: scm.Map[A, B]) extends ju.Dictionary[A, B] with Serializable {
  override def remove(key: Any): B = {
    try {
      underlying.remove(key.asInstanceOf[A]).getOrElse(null.asInstanceOf[B])
    } catch {
      case e: ClassCastException => null.asInstanceOf[B]
    }
  }
  override def put(key: A, value: B): B = underlying.put(key, value).getOrElse(null.asInstanceOf[B])
  override def get(key: Any): B = {
    try {
      underlying.get(key.asInstanceOf[A]).getOrElse(null.asInstanceOf[B])
    } catch {
      case e: ClassCastException => null.asInstanceOf[B]
    }
  }
  override def elements(): ju.Enumeration[B] = new IteratorWrapper(underlying.valuesIterator)
  override def keys(): ju.Enumeration[A] = new IteratorWrapper(underlying.keysIterator)
  override def isEmpty(): Boolean = underlying.isEmpty
  override def size(): Int = underlying.size
}
