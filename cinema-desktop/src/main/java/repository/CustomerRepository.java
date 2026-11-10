package repository;

import entity.Customer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerRepository {

    private static final String FILE_NAME = "database/customers.csv";

    public void save(List<Customer> customers) {
        File directory = new File("database");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter bw = StorageIO.writer(FILE_NAME)) {

            for (Customer customer : customers) {
                bw.write( customer.getId() + "," + customer.getName() + "," + customer.getEmail() + "," + customer.getPhone());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing customer data: " + e.getMessage(), e);
        }
    }

    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();

        try (BufferedReader br = StorageIO.reader(FILE_NAME)) {
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(",", -1);

                Customer customer = new Customer( Long.parseLong(data[0]), data[1], data[2], data[3]);

                customers.add(customer);
            }
        } catch (FileNotFoundException e){
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Customer data file not found.");
        } catch (IOException | NumberFormatException e){
            if (StorageIO.mysqlEnabled()) throw new IllegalStateException("MySQL read failed", e);
            System.out.println("Error reading customer data: " + e.getMessage());
        }

        return customers;
    }
}
