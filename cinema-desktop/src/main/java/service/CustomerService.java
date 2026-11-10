package service;

import entity.Customer;
import repository.CustomerRepository;

import java.util.List;

public class CustomerService {

    private final CustomerRepository repository;
    private final List<Customer> customers;

    public CustomerService() {
        repository = new CustomerRepository();
        customers = repository.findAll();
    }

    public void addCustomer(Customer customer) {
        customer.setId(getNextId());
        customers.add(customer);
        repository.save(customers);
    }

    public List<Customer> getAllCustomers() {
        return customers;
    }

    public Customer findByEmail(String email) {
        for (Customer customer : customers) {
            if (customer.getEmail().equalsIgnoreCase(email)) {
                return customer;
            }
        }
        return null;
    }

    public List<Customer> findByName(String name) {
        List<Customer> result = new java.util.ArrayList<>();

        for (Customer customer : customers) {
            if (customer.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(customer);
            }
        }

        return result;
    }

    public List<Customer> findByPhone(String phone) {
        List<Customer> result = new java.util.ArrayList<>();

        for (Customer customer : customers) {
            if (customer.getPhone().contains(phone)) {
                result.add(customer);
            }
        }

        return result;
    }

    public Customer findById(long id) {
        for (Customer customer : customers) {
            if (customer.getId() == id) {
                return customer;
            }
        }
        return null;
    }

    public void updateCustomer(Customer updatedCustomer) {
        Customer customer = findById(updatedCustomer.getId());

        if (customer != null) {
            customer.setName(updatedCustomer.getName());
            customer.setEmail(updatedCustomer.getEmail());
            customer.setPhone(updatedCustomer.getPhone());
            repository.save(customers);
        }
    }

    public void deleteCustomer(long id) {
        Customer customer = findById(id);

        if (customer != null) {
            customers.remove(customer);
            repository.save(customers);
        }
    }

    private long getNextId() {
        long maxId = 0;

        for (Customer customer : customers) {
            if (customer.getId() > maxId) {
                maxId = customer.getId();
            }
        }

        return maxId + 1;
    }
}
