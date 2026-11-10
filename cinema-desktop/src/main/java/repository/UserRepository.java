package repository;

import entity.Customer;
import entity.User;
import enums.Role;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private static final String FILE_NAME = "database/users.csv";

    public void save(List<User> users) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (User user : users) {
                long customerId = user.getCustomer() != null ? user.getCustomer().getId() : 0;

                bw.write(user.getId() + "," + user.getUsername() + "," + user.getPassword() + "," + user.getRole() + "," + customerId);
                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing user data: " + e.getMessage(), e);
        }
    }

    public List<User> findAll(List<Customer> customers) {
        List<User> users = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                long customerId = Long.parseLong(data[4]);
                Customer customer = null;

                for (Customer c : customers) {
                    if (c.getId() == customerId) {
                        customer = c;
                        break;
                    }
                }

                User user = new User(
                        Long.parseLong(data[0]),
                        data[1],
                        data[2],
                        Role.valueOf(data[3]),
                        customer
                );

                users.add(user);
            }
        } catch (FileNotFoundException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("User data file not found.");
        } catch (IOException | IllegalArgumentException e) {
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading user data: " + e.getMessage());
        }

        return users;
    }
}
