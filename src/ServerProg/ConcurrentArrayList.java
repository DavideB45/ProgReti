package ServerProg;

import java.util.ArrayList;

public class ConcurrentArrayList<E>{
    private final ArrayList<E> list = new ArrayList<>();

    public ConcurrentArrayList(){

    }

    public synchronized ArrayList<E> getListCopy(){
        ArrayList<E> copy = new ArrayList<>();
        for(E e : list){
            copy.add(e);
        }
        return copy;
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
    public synchronized void remove(int i){
        list.remove(i);
    }
    public synchronized void remove(E e){
        list.remove(e);
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
}
