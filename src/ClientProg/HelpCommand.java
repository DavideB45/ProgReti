package ClientProg;

public class HelpCommand {

    private static final String ALL_COMMANDS = "register\nlogin\nlogout\nfollow\nunfollow\npost\nshow post\n" +
            "show feed\nlist following\nlist followers\nlist users\nblog\ndelete\nrewin\nrate\ncomment\n" +
            "wallet\nwallet btc\nexit";
    private static final String REGISTER = "register <username> <password> <tags>\n" +
            "used to register a new user to the social network";
    private static final String LOGIN = "login <username> <password>\n" +
            "login username exists and if password is correct";
    private static final String LOGOUT = "logout\n";
    private static final String FOLLOW = "follow <username>" +
            "\nfollow username if exists and if you are not already following him/her";
    private static final String UNFOLLOW = "unfollow <username>\n" +
            "unfollow username if exists and if you are following him/her";
    private static final String POST = "post <\"title\"><\"text\">\n" +
            "post a new post with title and text";
    private static final String SHOW_POST = "show post <id>\n" +
            "show complete information of post with id";
    private static final String SHOW_FEED = "show feed\n" +
            "show simplified versions of posts of all users you are following";
    private static final String LIST_FOLLOWING = "list following\n" +
            "show all users you are following";
    private static final String LIST_FOLLOWERS = "list followers\n" +
            "show all users following you";
    private static final String LIST_USERS = "list users\n" +
            "show all users registered in the social network with at least tag in common with you";
    private static final String BLOG = "blog\n" +
            "show a simplified version of all posts of your blog";
    private static final String DELETE = "delete <id>\n" +
            "delete post with id if you are the author\n" + "remove post from your blog if you made a rewin";
    private static final String REWIN = "rewin <id>\n" +
            "rewin post with id if you are not the author\n";
    private static final String RATE = "rate <id> <rating>\n" +
            "rate post with id with +1 or -1";
    private static final String COMMENT = "comment <id> <comment>\n" +
            "comment post with id with comment";
    private static final String WALLET = "wallet\n" +
            "show your wallet";
    private static final String WALLET_BTC = "wallet btc\n" +
            "show the value of your wallet in bitcoin";
    private static final String EXIT = "exit\n" +
            "exit the program";

    public HelpCommand(){}

    public String getHelp(String command){
        if(command == null){
            return ALL_COMMANDS;
        }
        switch (command){
            case "":
                return ALL_COMMANDS;
            case "register":
                return REGISTER;
            case "login":
                return LOGIN;
            case "logout":
                return LOGOUT;
            case "follow":
                return FOLLOW;
            case "unfollow":
                return UNFOLLOW;
            case "post":
                return POST;
            case "show post":
                return SHOW_POST;
            case "show feed":
                return SHOW_FEED;
            case "list following":
                return LIST_FOLLOWING;
            case "list followers":
                return LIST_FOLLOWERS;
            case "list users":
                return LIST_USERS;
            case "blog":
                return BLOG;
            case "delete":
                return DELETE;
            case "rewin":
                return REWIN;
            case "rate":
                return RATE;
            case "comment":
                return COMMENT;
            case "wallet":
                return WALLET;
            case "wallet btc":
                return WALLET_BTC;
            case "exit":
                return EXIT;
            default:
                return "Command not found";
        }
    }
}
