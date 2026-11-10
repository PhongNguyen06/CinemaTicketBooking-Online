package service;

import entity.Customer;
import entity.User;
import enums.Role;
import repository.UserRepository;
import security.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.util.List;

public class UserService {

    private final UserRepository repository;
    private final List<User> users;
    private final List<Customer> customers;

    public UserService(List<Customer> customers) {
        this.customers = customers;
        repository = new UserRepository();
        users = repository.findAll(customers);

        createDefaultAdmin();
    }

    public User login(String username, String password) {
        for (User user : users) {
            if (!user.getUsername().equals(username)) continue;
            String stored = user.getPassword();
            if (PasswordHasher.isHashed(stored)) {
                return PasswordHasher.verify(password, stored) ? user : null;
            }
            // Backward compatibility: upgrade only the successfully authenticated legacy user.
            if (MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
                    stored.getBytes(StandardCharsets.UTF_8))) {
                String oldPassword = stored;
                user.setPassword(PasswordHasher.hash(password));
                try {
                    repository.save(users);
                } catch (RuntimeException e) {
                    user.setPassword(oldPassword);
                    throw new IllegalStateException("Could not upgrade password storage; login denied", e);
                }
                return user;
            }
            return null;
        }

        return null;
    }

    /** Changes the authenticated account password without modifying other accounts. */
    public synchronized boolean changePassword(User account, String currentPassword, String newPassword) {
        if (account == null || currentPassword == null || newPassword == null) return false;
        if (newPassword.length() < 12 || newPassword.length() > 128) {
            throw new IllegalArgumentException("New password must contain 12 to 128 characters.");
        }
        User persisted = null;
        for (User user : users) {
            if (user.getId() == account.getId() && user.getUsername().equals(account.getUsername())) {
                persisted = user;
                break;
            }
        }
        if (persisted == null) return false;
        String previous = persisted.getPassword();
        boolean valid = PasswordHasher.isHashed(previous)
                ? PasswordHasher.verify(currentPassword, previous)
                : MessageDigest.isEqual(currentPassword.getBytes(StandardCharsets.UTF_8),
                        previous.getBytes(StandardCharsets.UTF_8));
        if (!valid || currentPassword.equals(newPassword)) return false;
        persisted.setPassword(PasswordHasher.hash(newPassword));
        try {
            repository.save(users);
            if (persisted != account) account.setPassword(persisted.getPassword());
            return true;
        } catch (RuntimeException ex) {
            persisted.setPassword(previous);
            if (persisted != account) account.setPassword(previous);
            throw new IllegalStateException("Could not save password change", ex);
        }
    }

    public User findByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }

        return null;
    }

    public User registerUser(String username, String password, Customer customer) {
        if (findByUsername(username) != null) {
            return null;
        }

        User user = new User();
        user.setId(getNextId());
        user.setUsername(username);
        user.setPassword(PasswordHasher.hash(password));
        user.setRole(Role.USER);
        user.setCustomer(customer);

        users.add(user);
        repository.save(users);

        return user;
    }

    public List<User> getAllUsers() {
        return users;
    }

    private void createDefaultAdmin() {
        if (findAdmin() == null) {
            User admin = new User();
            admin.setId(getNextId());
            admin.setUsername("admin");
            String initialPassword = System.getenv("CINEMA_INITIAL_ADMIN_PASSWORD");
            if (initialPassword == null || initialPassword.length() < 12) {
                throw new IllegalStateException(
                    "First run requires CINEMA_INITIAL_ADMIN_PASSWORD (at least 12 characters). " +
                    "Existing accounts are not affected.");
            }
            admin.setPassword(PasswordHasher.hash(initialPassword));
            admin.setRole(Role.ADMIN);
            admin.setCustomer(null);

            users.add(admin);
            repository.save(users);
        }
    }

    private User findAdmin() {
        for (User user : users) {
            if (user.getRole() == Role.ADMIN) {
                return user;
            }
        }

        return null;
    }

    private long getNextId() {
        long maxId = 0;

        for (User user : users) {
            if (user.getId() > maxId) {
                maxId = user.getId();
            }
        }

        return maxId + 1;
    }
}
