package ServerProg;


import java.util.ArrayList;

public class MainServer {
    public static void main(String[] args){
        System.out.println("hello world");

        ConcurrentArrayList<Post> posts = new ConcurrentArrayList<>();
        posts.add(new Post(0, "admin", "titolo", "testo"));

        System.out.println(posts.get(0).getTitle());
        System.out.println(posts.get(0).getText());
        System.out.println(posts.get(0).getCreator());
        System.out.println(posts.get(0).getUpVotes());
        System.out.println();

        ArrayList<Post> posts2 = posts.getListCopy();
        posts2.get(0).vote("admin", 1);
        posts2.get(0).vote("admin2", 1);
        posts2.get(0).vote("admin3", 1);
        System.out.println("in copy : " + posts2.get(0).getUpVotes());
        System.out.println("in orig : " + posts.get(0).getUpVotes());

    }
}
