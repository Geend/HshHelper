package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class User {

    public int id;
    public String userName;
    public String email;
    public String password;
    public boolean passwordResetRequired;
    public int quotaLimit;

    public User(
            int id,
            String userName,
            String email,
            String password,
            boolean passwordResetRequired,
            int quotaLimit) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.passwordResetRequired = passwordResetRequired;
        this.quotaLimit = quotaLimit;
    }

    public User() {
    }

    // note: only testcode for the first day, switch later to in memory
    // database h2
    private static List<User> users;

    static {
        users = new ArrayList<User>();
        users.add(new User(0, "admin", "admin@admin.de", "admin", true, 0));
        users.add(new User(1, "peter", "peter@web.de", "peter", true, 0));
    }

    public static List<User> findAll() {
        return new ArrayList<User>(users);
    }

    public static void add(User newUser) {
        users.add(newUser);
    }

    public static Optional<User> findById(Integer id) {
        return users.stream().filter(x -> x.id == id).findFirst();
    }

    public static Optional<User> findByName(String username){
        return users.stream().filter(x -> x.userName.equals(username)).findFirst();
    }
    public static boolean authenticate(String username, String password)
    {
        return users.stream().filter(x -> x.userName.equals(username) && x.password.equals(password)).findAny().isPresent();
    }

    public static User getById(int id) {
        return users.get(id);
    }
}
