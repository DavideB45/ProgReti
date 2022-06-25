package ServerProg;

import java.util.ArrayList;

public class ConcurrentArrayList<E>{
    private final ArrayList<E> list = new ArrayList<>();

    public ConcurrentArrayList(){

    }
    public ConcurrentArrayList(ArrayList<E> list){
        this.list.addAll(list);
    }

    /**
     * return a shallow copy of the list
     * */
    public synchronized ArrayList<E> getListCopy(){
        return new ArrayList<>(list);
    }
    /**
     * add element in the collection
     */
    public synchronized void add(E e){
        list.add(e);
    }
    /**
     * get the element to a given  position
     * */
    public synchronized E get(int i) throws IndexOutOfBoundsException{
        return list.get(i);
    }
    /**
     * return the number of elements in the collection
     */
    public synchronized int size(){
        return list.size();
    }
    /**
     * remove element in a given position
     */
    public synchronized void removeAt(int i){
        list.remove(i);
    }
    /**
     * remove the first occurrence of given element
     */
    public synchronized boolean removeElement(E e){
        return list.remove(e);
    }
    /**
     * returns true if and only if this collection contains at least
     * one element e such that Objects.equals(o, e)
     */
    public synchronized boolean contains(E e){
        return list.contains(e);
    }
    /**
     * if the collection contains the element return false
     * else add the element and return true
     */
    public synchronized boolean addIfAbsent(E e){
        if (list.contains(e)){
            return false;
        } else {
            return list.add(e);
        }
    }
    /**
     * remove all elements
     */
    public synchronized void clear(){
        list.clear();
    }
    /**
     * add all elements to the collection
     */
    public synchronized void addAll(ArrayList<E> list){
        this.list.addAll(list);
    }
}
