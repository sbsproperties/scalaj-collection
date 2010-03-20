package scalaj
package collection
package j2s

import java.{lang => jl, util => ju}
import scala.{collection => sc}
import scala.collection.{immutable => sci, mutable => scm}

object Wrappers {
  def EnumerationWrapper[A](underlying: ju.Enumeration[A]) = new EnumerationWrapper(underlying)
  def IteratorWrapper[A](underlying: ju.Iterator[A]) = new IteratorWrapper(underlying)
  def IterableWrapper[A](underlying: jl.Iterable[A]) = new IterableWrapper(underlying)
  def CollectionWrapper[A](underlying: ju.Collection[A]) = new CollectionWrapper(underlying)
  def ListWrapper[A](underlying: ju.List[A]) = new ListWrapper(underlying)
  def MutableListWrapper[A](underlying: ju.List[A]) = new MutableListWrapper(underlying)
  def SetWrapper[A](underlying: ju.Set[A]) = new SetWrapper(underlying)
  def MutableSetWrapper[A](underlying: ju.Set[A]) = new MutableSetWrapper(underlying)
  def MapWrapper[A, B](underlying: ju.Map[A, B]) = new MapWrapper(underlying)
  def MutableMapWrapper[A, B](underlying: ju.Map[A, B]) = new MutableMapWrapper(underlying)
}

class EnumerationWrapper[A](val underlying: ju.Enumeration[A]) extends sc.Iterator[A] {
  override def next: A = underlying.nextElement
  override def hasNext: Boolean = underlying.hasMoreElements
}

class IteratorWrapper[A](val underlying: ju.Iterator[A]) extends sc.Iterator[A] {
  override def next: A = underlying.next
  override def hasNext: Boolean = underlying.hasNext
}

class IterableWrapper[A](val underlying: jl.Iterable[A]) extends sc.Iterable[A] {
  override def iterator: sc.Iterator[A] = new IteratorWrapper(underlying.iterator)
}

class CollectionWrapper[A](override val underlying: ju.Collection[A]) extends IterableWrapper(underlying)

class ListWrapper[A](override val underlying: ju.List[A]) extends CollectionWrapper(underlying) with sc.Seq[A] {
  override def apply(index: Int): A = underlying.get(index)
  override def length: Int = underlying.size
}

class MutableListWrapper[A](override val underlying: ju.List[A]) extends ListWrapper(underlying) with scm.Seq[A] {
  override def update(index: Int, element: A): Unit = underlying.set(index, element)
}

abstract class AbstractSetWrapper[A](override val underlying: ju.Set[A]) extends CollectionWrapper(underlying) with sc.Set[A] {
  override def contains(key: A): Boolean = underlying.contains(key)
}

class SetWrapper[A](override val underlying: ju.Set[A]) extends AbstractSetWrapper(underlying) {
  override def - (elem: A): sc.Set[A] = empty ++ this - elem
  override def + (elem: A): sc.Set[A] = empty ++ this + elem
}

class MutableSetWrapper[A](override val underlying: ju.Set[A]) extends AbstractSetWrapper(underlying) with scm.Set[A] {
  override def -= (elem: A): this.type = {
    underlying.remove(elem)
    this
  }
  override def += (elem: A): this.type = {
    underlying.add(elem)
    this
  }
}

abstract class AbstractMapWrapper[A, B](val underlying: ju.Map[A, B]) extends sc.Map[A, B] {
  override def iterator: sc.Iterator[(A, B)] =
    new IteratorWrapper(underlying.entrySet.iterator).map(entry => (entry.getKey, entry.getValue))  
  override def get(key: A): Option[B] = underlying.get(key) match {
    case null => if (underlying.containsKey(key)) Some(null.asInstanceOf[B]) else None
    case value => Some(value)
  }
}

class MapWrapper[A, B](override val underlying: ju.Map[A, B]) extends AbstractMapWrapper(underlying) {
  override def - (key: A): sc.Map[A, B] = empty ++ this - key
  override def + [B1 >: B](kv: (A, B1)): sc.Map[A, B1] = empty ++ this + (kv)
}

class MutableMapWrapper[A, B](override val underlying: ju.Map[A, B]) extends AbstractMapWrapper(underlying) with scm.Map[A, B] {
  override def -= (key: A): this.type = {
    underlying.remove(key)
    this
  }
  override def += (kv: (A, B)): this.type = {
    underlying.put(kv._1, kv._2)
    this
  }
}
