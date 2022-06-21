package ServerProg;

import java.util.ArrayList;

public class ConcurrentArrayList<E>{
    private final ArrayList<E> list = new ArrayList<>();

    public ConcurrentArrayList(){

    }
    public ConcurrentArrayList(ArrayList<E> list){
        this.list.addAll(list);
    }

    public synchronized ArrayList<E> getListCopy(){
        return new ArrayList<>(list);
    }
    public synchronized void add(E e){
        list.add(e);
    }
    public synchronized E get(int i) throws IndexOutOfBoundsException{
        return list.get(i);
    }
    public synchronized int size(){
        return list.size();
    }
    public synchronized void removeAt(int i){
        list.remove(i);
    }
    public synchronized boolean removeElement(E e){
        return list.remove(e);
    }
    public synchronized boolean contains(E e){
        return list.contains(e);
    }
    public synchronized boolean addIfAbsent(E e){
        if (list.contains(e)){
            return false;
        } else {
            return list.add(e);
        }
    }
    public synchronized void clear(){
        list.clear();
    }
    public synchronized void addAll(ArrayList<E> list){
        this.list.addAll(list);
    }
}
