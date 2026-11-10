package entity;

import enums.Role;

public class User {
    private long id;
    private String username;
    private String password;
    private Role role;
    private Customer customer;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public User(long id, String username, String password, Role role, Customer customer) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.customer = customer;
    }

    public User() {
    }
}
