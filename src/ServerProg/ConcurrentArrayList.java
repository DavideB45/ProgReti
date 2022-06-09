package ServerProg;

import java.util.ArrayList;

public class ConcurrentArrayList<E> {
    private ArrayList<E> list;

    public ConcurrentArrayList(){
        list = new ArrayList<E>();
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
