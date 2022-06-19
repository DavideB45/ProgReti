package ServerProg;

import ClientProg.PostHead;
import ClientProg.SimplePost;
import ClientProg.SimpleUtente;
import ClientProg.SimpleWallet;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;

public class ClientRequestRunnable implements Runnable{
    private final ConnectedUser u;
    private final Selector selector;
    private final WncBtcCalculator exchanger;
    private final SocialNetwork sn;

    public ClientRequestRunnable(ConnectedUser user, Selector sel, WncBtcCalculator exchanger, SocialNetwork sn){
        this.selector = sel;
        u = user;
        this.exchanger = exchanger;
        this.sn = sn;
    }

    @Override
    public void run() {
        try {
            if(u.getKey().isReadable()){
                if(u.readRequest()){
                    // request is fully read
                    System.out.println("Request fully read");
                    int opCode = u.getOperation();
                    String[] args = u.getArgs();
                    if(! handleUser(opCode, args)){
                        u.getKey().cancel();
                    } else {
                        u.getKey().interestOps(SelectionKey.OP_WRITE);
                        selector.wakeup();
                    }
                } else {
                    u.getKey().interestOps(SelectionKey.OP_READ);
                    selector.wakeup();
                }
            } else if (u.getKey().isWritable()){
                if(u.sendResponse()){
                    System.out.println("Response sent");
                    u.getKey().interestOps(SelectionKey.OP_READ);
                    selector.wakeup();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // close connection
        }
    }

    private boolean handleUser(int operation, String[] args) {
        if (!u.isConnected()){
            return false;
        }
        try {
            Utente user = u.getIdentity();
            ObjectMapper mapper = new ObjectMapper();
            switch (operation){
                case 2:
                    Utente verifiedUser = sn.login(args[0], args[1]);
                    u.setIdentity(verifiedUser);
                    if (verifiedUser == null) {
                        u.setResponse(404, null);
                    } else {
                        u.setResponse(200, new String[]{"238.255.1.3", "3000"});
                    }
                    break;
                case 3:
                    u.setResponse(sn.logout(user), null);
                    u.setIdentity(null);
                    break;
                case 7:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        u.setResponse(sn.follow(user, args[0]), null);
                    }
                    break;
                case 6:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        ArrayList<SimpleUtente> list = sn.getFollowing(user);
                        if (list == null) {
                            u.setResponse(404, null);
                        } else {
                            String jsonList = mapper.writeValueAsString(list);
                            u.setResponse(200, new String[]{jsonList});
                        }
                    }
                    break;
                case 8:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        u.setResponse(sn.unfollow(user, args[0]), null);
                    }
                    break;
                case 9:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        ArrayList<PostHead> posts = sn.getPosts(user);
                        if (posts == null) {
                            u.setResponse(404, null);
                        } else {
                            String jsonList = mapper.writeValueAsString(posts);
                            u.setResponse(200, new String[]{jsonList});
                        }
                    }
                    break;
                case 10:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        SimplePost post = mapper.readValue(args[0], SimplePost.class);
                        int num = sn.post(user,post);
                        if (num == -1) {
                            u.setResponse(400, null);
                        } else {
                            u.setResponse(200, new String[]{String.valueOf(num)});
                        }
                    }
                    break;
                case 11:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        ArrayList<PostHead> posts = sn.showFeed(user);
                        if (posts == null) {
                            u.setResponse(500, null);
                        } else {
                            String jsonList = mapper.writeValueAsString(posts);
                            u.setResponse(200, new String[]{jsonList});
                        }
                    }
                    break;
                case 12:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        Post post = sn.getPost(Integer.parseInt(args[0]));
                        if (post == null) {
                            u.setResponse(404, null);
                        } else {
                            u.setResponse(200, new String[]{mapper.writeValueAsString(new SimplePost(post))});
                        }
                    }
                    break;
                case 13:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        u.setResponse(sn.deletePost(user, Integer.parseInt(args[0])), null);
                    }
                    break;
                case 14:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        u.setResponse(sn.rewin(user, Integer.parseInt(args[0])), null);
                    }
                    break;
                case 15:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        u.setResponse(sn.ratePost(user, Integer.parseInt(args[0]), Integer.parseInt(args[1])), null);
                    }
                    break;
                case 16:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        u.setResponse(sn.comment(user, Integer.parseInt(args[0]), args[1]), null);
                    }
                    break;
                case 17:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        SimpleWallet wallet = sn.getWallet(user);
                        if (wallet == null) {
                            u.setResponse(500, null);
                        } else {
                            System.out.println(mapper.writeValueAsString(wallet));
                            u.setResponse(200, new String[]{mapper.writeValueAsString(wallet)});
                        }
                    }
                    break;
                case 18:
                    if(user == null){
                        u.setResponse(401, null);
                    } else {
                        float btc = sn.getWincoin(user);
                        if (btc >= 0) {
                            btc = exchanger.WNCtoBTC(btc);
                            if (btc == -1)
                                u.setResponse(503, null);
                            else
                                u.setResponse(200, new String[]{String.valueOf(btc)});
                        }
                        else
                            u.setResponse(500, null);
                    }
                    break;
                default:
                    System.out.println("richiesta strana : " + operation);
                    for (String arg: args) {
                        System.out.println(arg);
                    }
                    u.setResponse(418, null);
                    break;
            }
        } catch (JsonMappingException | JsonParseException | NumberFormatException e) {
            e.printStackTrace();
            try {
                u.setResponse(400, null);
            } catch (IOException ex) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}